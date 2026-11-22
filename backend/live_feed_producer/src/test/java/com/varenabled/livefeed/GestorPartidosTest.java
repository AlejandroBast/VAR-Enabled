package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GestorPartidosTest {

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class RelojDePrueba extends Clock {
        private Instant ahora = Instant.parse("2026-11-22T20:00:00Z");

        void avanzarMinutos(long minutos) {
            ahora = ahora.plus(Duration.ofMinutes(minutos));
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private RelojDePrueba reloj;
    private GestorPartidos gestor;

    @BeforeEach
    void preparar() {
        reloj = new RelojDePrueba();
        // 1 minuto de partido = 60 segundos reales.
        gestor = new GestorPartidos(kafkaTemplate, reloj, 60);
    }

    private void crearEIniciar(String matchId) {
        gestor.crearPartido(matchId);
        gestor.iniciarPartido(matchId);
    }

    @Test
    void unPartidoCreadoApareceEnElListadoComoPorJugar() {
        gestor.crearPartido("101");

        assertEquals(List.of(new ResumenPartido("101", "POR_JUGAR", null, null)), gestor.listarPartidos());
    }

    @Test
    void crearElMismoPartidoDosVecesNoLoDuplica() {
        gestor.crearPartido("101");
        gestor.crearPartido("101");

        assertEquals(1, gestor.listarPartidos().size());
    }

    @Test
    void iniciarElPartidoPublicaElInicioEnElMinutoUno() {
        gestor.crearPartido("101");

        EventoPartidoDto inicio = gestor.iniciarPartido("101");

        assertEquals("MATCH_STARTED", inicio.eventType());
        assertEquals(1, inicio.minuto());
        verify(kafkaTemplate).send(eq("match_events"), eq("101"), eq(inicio));
    }

    @Test
    void noSePuedeIniciarDosVecesElMismoPartido() {
        crearEIniciar("101");

        assertThrows(IllegalStateException.class, () -> gestor.iniciarPartido("101"));
    }

    @Test
    void noSePuedeMandarUnEventoAntesDeQueEmpieceElPartido() {
        gestor.crearPartido("101");

        assertThrows(IllegalStateException.class,
                () -> gestor.registrarEvento("101", "GOAL", "home", null, null));
    }

    @Test
    void registrarUnEventoParaUnPartidoNoCreadoLanzaExcepcion() {
        assertThrows(PartidoNoEncontradoException.class,
                () -> gestor.registrarEvento("999", "GOAL", "home", null, null));
    }

    @Test
    void elGolTomaElMinutoQueMarcaElRelojEnEseMomento() {
        crearEIniciar("101");
        reloj.avanzarMinutos(22);

        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);

        assertEquals(23, gol.minuto());
        assertEquals(0, gol.minutoExtra());
        assertEquals("home", gol.team());
        verify(kafkaTemplate).send(eq("match_events"), eq("101"), eq(gol));
    }

    @Test
    void elListadoMuestraElMinutoQueVaCorriendo() {
        crearEIniciar("101");
        reloj.avanzarMinutos(30);

        assertEquals(new ResumenPartido("101", "EN_JUEGO", 31, 0), gestor.listarPartidos().get(0));
    }

    @Test
    void losEventosDelRelojNoSePuedenMandarAMano() {
        crearEIniciar("101");

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "CLOCK_TICK", null, null, null));
    }

    @Test
    void registrarUnaAnulacionConservaElAnnulsEventId() {
        crearEIniciar("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);

        EventoPartidoDto anulacion = gestor.registrarEvento("101", "GOAL_ANNULLED", null, gol.eventId(), null);

        assertEquals(gol.eventId(), anulacion.annulsEventId());
    }

    @Test
    void losEventosQuedanEnElHistorialEnOrden() {
        gestor.crearPartido("101");
        EventoPartidoDto inicio = gestor.iniciarPartido("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);
        EventoPartidoDto check = gestor.registrarEvento("101", "VAR_CHECK", null, null, null);

        assertEquals(List.of(inicio, gol, check), gestor.listarEventos("101"));
    }

    @Test
    void losEventIdNoSeRepiten() {
        crearEIniciar("101");
        EventoPartidoDto primero = gestor.registrarEvento("101", "GOAL", "home", null, null);
        EventoPartidoDto segundo = gestor.registrarEvento("101", "GOAL", "away", null, null);

        assertNotEquals(primero.eventId(), segundo.eventId());
    }

    @Test
    void elTiempoAnadidoSoloSePuedeAnunciarEnElUltimoMinutoDeUnaMitad() {
        crearEIniciar("101");
        reloj.avanzarMinutos(29);

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("101", "ADDED_TIME", null, null, 3));
    }

    @Test
    void anunciarElTiempoAnadidoSinMinutosAgregadosLanzaExcepcion() {
        crearEIniciar("101");
        reloj.avanzarMinutos(44);

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "ADDED_TIME", null, null, null));
    }

    @Test
    void despuesDelTiempoAnadidoUnGolCaeEnElMinutoCuarentaYCincoMasAlgo() {
        crearEIniciar("101");
        reloj.avanzarMinutos(44);
        gestor.registrarEvento("101", "ADDED_TIME", null, null, 3);
        reloj.avanzarMinutos(2);

        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);

        assertEquals(45, gol.minuto());
        assertEquals(2, gol.minutoExtra());
    }

    @Test
    void cuandoCambiaElMinutoElRelojAvisaPorKafka() {
        crearEIniciar("101");
        reloj.avanzarMinutos(1);

        gestor.avanzarRelojes();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, atLeastOnce()).send(eq("match_events"), eq("101"), captor.capture());
        EventoPartidoDto tick = (EventoPartidoDto) captor.getValue();
        assertEquals("CLOCK_TICK", tick.eventType());
        assertEquals(2, tick.minuto());
    }

    @Test
    void siElMinutoNoCambioNoSeMandaNada() {
        crearEIniciar("101");

        gestor.avanzarRelojes();

        // Solo el MATCH_STARTED, ningun CLOCK_TICK.
        verify(kafkaTemplate, times(1)).send(eq("match_events"), eq("101"), any());
    }

    @Test
    void alCumplirseElTiempoElPartidoTerminaYNoAceptaMasEventos() {
        crearEIniciar("101");
        reloj.avanzarMinutos(90);

        gestor.avanzarRelojes();

        List<EventoPartidoDto> historial = gestor.listarEventos("101");
        assertEquals("MATCH_ENDED", historial.get(historial.size() - 1).eventType());
        assertEquals("FINALIZADO", gestor.listarPartidos().get(0).estado());
        assertThrows(IllegalStateException.class,
                () -> gestor.registrarEvento("101", "GOAL", "home", null, null));
    }
}
