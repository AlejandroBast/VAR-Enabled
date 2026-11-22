package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoInvalidoException;
import com.varenabled.dominio.futbol.ReglaPartidoException;
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
        gestor.crearPartido(matchId, "Nacional", "América");
        gestor.iniciarPartido(matchId);
    }

    /** Avanza el reloj hasta que el partido marque ese minuto (primer tiempo). */
    private void irAlMinuto(int minuto) {
        reloj.avanzarMinutos(minuto - 1);
        gestor.avanzarRelojes();
    }

    private EventoPartidoDto gol(String team) {
        return gestor.registrarEvento("101", "GOAL", team, null, null, null);
    }

    private EventoPartidoDto ultimoEvento() {
        List<EventoPartidoDto> historial = gestor.listarEventos("101");
        return historial.get(historial.size() - 1);
    }

    // ---- Creacion e inicio ----

    @Test
    void unPartidoCreadoApareceComoPorJugarConSusEquipos() {
        gestor.crearPartido("101", "Nacional", "América");

        ResumenPartido resumen = gestor.listarPartidos().get(0);
        assertEquals("POR_JUGAR", resumen.estado());
        assertEquals("Nacional", resumen.homeTeam());
        assertEquals("América", resumen.awayTeam());
    }

    @Test
    void crearUnPartidoAvisaPorKafkaConLosNombresDeLosEquipos() {
        gestor.crearPartido("101", "Nacional", "América");

        EventoPartidoDto creacion = gestor.listarEventos("101").get(0);
        assertEquals("MATCH_CREATED", creacion.eventType());
        assertEquals("Nacional", creacion.homeTeam());
        assertEquals("América", creacion.awayTeam());
        assertNull(creacion.minuto());
        verify(kafkaTemplate).send(eq("match_events"), eq("101"), eq(creacion));
    }

    @Test
    void crearElMismoPartidoDosVecesNoLoDuplicaNiLoVuelveAAvisar() {
        gestor.crearPartido("101", null, null);
        gestor.crearPartido("101", null, null);

        assertEquals(1, gestor.listarPartidos().size());
        verify(kafkaTemplate, times(1)).send(eq("match_events"), eq("101"), any());
    }

    @Test
    void noSePuedeIniciarDosVecesElMismoPartido() {
        crearEIniciar("101");

        assertThrows(IllegalStateException.class, () -> gestor.iniciarPartido("101"));
    }

    @Test
    void noSePuedeMandarUnEventoAntesDeQueEmpieceElPartido() {
        gestor.crearPartido("101", null, null);

        assertThrows(IllegalStateException.class, () -> gol("home"));
    }

    @Test
    void registrarUnEventoParaUnPartidoNoCreadoLanzaExcepcion() {
        assertThrows(PartidoNoEncontradoException.class,
                () -> gestor.registrarEvento("999", "GOAL", "home", null, null, null));
    }

    // ---- Validaciones de los eventos ----

    @Test
    void elGolTomaElMinutoQueMarcaElRelojEnEseMomento() {
        crearEIniciar("101");
        irAlMinuto(23);

        EventoPartidoDto gol = gol("home");

        assertEquals(23, gol.minuto());
        assertEquals("home", gol.team());
    }

    @Test
    void losEventosDelSistemaNoSePuedenMandarAMano() {
        crearEIniciar("101");

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "CLOCK_TICK", null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "GOAL_CONFIRMED", null, null, null, null));
    }

    @Test
    void golesFaltasYTarjetasNecesitanEquipo() {
        crearEIniciar("101");

        assertThrows(IllegalArgumentException.class, () -> gestor.registrarEvento("101", "YELLOW_CARD", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> gestor.registrarEvento("101", "GOAL", "nadie", null, null, null));
    }

    @Test
    void lasTarjetasSeRegistranConSuEquipo() {
        crearEIniciar("101");

        EventoPartidoDto roja = gestor.registrarEvento("101", "RED_CARD", "away", null, null, null);

        assertEquals("RED_CARD", roja.eventType());
        assertEquals("away", roja.team());
    }

    @Test
    void losEventIdNoSeRepiten() {
        crearEIniciar("101");

        assertNotEquals(gol("home").eventId(), gol("away").eventId());
    }

    // ---- Reglas del VAR ----

    @Test
    void unGolAl38SePuedeRevisarEnEl40() {
        crearEIniciar("101");
        irAlMinuto(38);
        EventoPartidoDto gol = gol("home");
        reloj.avanzarMinutos(2);

        EventoPartidoDto revision = gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null);

        assertEquals(40, revision.minuto());
        assertEquals(gol.eventId(), revision.reviewsEventId());
        assertEquals("EN_REVISION", gestor.listarPartidos().get(0).estado());
    }

    @Test
    void unGolAl38YaNoSePuedeRevisarEnEl41() {
        crearEIniciar("101");
        irAlMinuto(38);
        EventoPartidoDto gol = gol("home");
        reloj.avanzarMinutos(3);

        assertThrows(ReglaPartidoException.class,
                () -> gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null));
    }

    @Test
    void revisarSinIndicarLaJugadaEsUnError() {
        crearEIniciar("101");

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "VAR_CHECK", null, null, null, null));
    }

    @Test
    void duranteLaRevisionElJuegoEstaParado() {
        crearEIniciar("101");
        EventoPartidoDto gol = gol("home");
        gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null);

        assertThrows(ReglaPartidoException.class, () -> gol("away"));
        assertThrows(ReglaPartidoException.class,
                () -> gestor.registrarEvento("101", "YELLOW_CARD", "home", null, null, null));
    }

    @Test
    void matchContinuesSoloSiHayRevisionYConfirmaLaJugadaRevisada() {
        crearEIniciar("101");
        EventoPartidoDto gol = gol("home");

        assertThrows(ReglaPartidoException.class,
                () -> gestor.registrarEvento("101", "MATCH_CONTINUES", null, null, null, null));

        gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null);
        EventoPartidoDto sigue = gestor.registrarEvento("101", "MATCH_CONTINUES", null, null, null, null);

        assertEquals(gol.eventId(), sigue.reviewsEventId());
        assertEquals("EN_JUEGO", gestor.listarPartidos().get(0).estado());
    }

    @Test
    void soloSePuedeAnularUnGolQueEsteEnRevision() {
        crearEIniciar("101");
        EventoPartidoDto gol = gol("home");

        assertThrows(ReglaPartidoException.class,
                () -> gestor.registrarEvento("101", "GOAL_ANNULLED", null, gol.eventId(), null, null));

        gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null);
        EventoPartidoDto anulacion = gestor.registrarEvento("101", "GOAL_ANNULLED", null, null, null, null);

        assertEquals(gol.eventId(), anulacion.annulsEventId());
    }

    @Test
    void unaFaltaSeAnulaIgualQueUnGol() {
        crearEIniciar("101");
        EventoPartidoDto falta = gestor.registrarEvento("101", "FOUL", "away", null, null, null);
        gestor.registrarEvento("101", "VAR_CHECK", null, null, falta.eventId(), null);

        assertThrows(ReglaPartidoException.class,
                () -> gestor.registrarEvento("101", "GOAL_ANNULLED", null, null, null, null));

        EventoPartidoDto anulacion = gestor.registrarEvento("101", "FOUL_ANNULLED", null, null, null, null);
        assertEquals(falta.eventId(), anulacion.annulsEventId());
    }

    @Test
    void unGolQueNadieRevisaQuedaFirmeAlPasarSusDosMinutos() {
        crearEIniciar("101");
        irAlMinuto(38);
        EventoPartidoDto gol = gol("home");

        reloj.avanzarMinutos(2);
        gestor.avanzarRelojes();
        assertEquals("GOAL", ultimoEvento().eventType());

        reloj.avanzarMinutos(1);
        gestor.avanzarRelojes();
        assertEquals("GOAL_CONFIRMED", ultimoEvento().eventType());
        assertEquals(gol.eventId(), ultimoEvento().reviewsEventId());
    }

    @Test
    void elListadoMuestraQueJugadasSePuedenRevisar() {
        crearEIniciar("101");
        EventoPartidoDto gol = gol("home");

        assertEquals(List.of(gol.eventId()), gestor.listarPartidos().get(0).revisables());
    }

    // ---- Tiempo anadido, entretiempo y final ----

    @Test
    void elTiempoAnadidoSoloSePuedeAnunciarEnElUltimoMinutoDeUnaMitad() {
        crearEIniciar("101");
        irAlMinuto(30);

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("101", "ADDED_TIME", null, null, null, 3));
    }

    @Test
    void despuesDelTiempoAnadidoUnGolCaeEnElMinutoCuarentaYCincoMasAlgo() {
        crearEIniciar("101");
        irAlMinuto(45);
        gestor.registrarEvento("101", "ADDED_TIME", null, null, null, 3);
        reloj.avanzarMinutos(2);

        EventoPartidoDto gol = gol("home");

        assertEquals(45, gol.minuto());
        assertEquals(2, gol.minutoExtra());
    }

    @Test
    void alTerminarElPrimerTiempoVieneElEntretiempoYSeFrenaElJuego() {
        crearEIniciar("101");
        irAlMinuto(46);

        assertEquals("HALF_TIME", ultimoEvento().eventType());
        assertEquals("ENTRETIEMPO", gestor.listarPartidos().get(0).estado());
        assertThrows(IllegalStateException.class, () -> gol("home"));
    }

    @Test
    void elSegundoTiempoArrancaEnEl46() {
        crearEIniciar("101");
        irAlMinuto(46);
        reloj.avanzarMinutos(15);

        EventoPartidoDto segundoTiempo = gestor.iniciarSegundoTiempo("101");

        assertEquals("SECOND_HALF_STARTED", segundoTiempo.eventType());
        assertEquals(46, segundoTiempo.minuto());
        assertEquals(46, gol("home").minuto());
    }

    @Test
    void noSePuedeIniciarElSegundoTiempoSiNoEsEntretiempo() {
        crearEIniciar("101");

        assertThrows(IllegalStateException.class, () -> gestor.iniciarSegundoTiempo("101"));
    }

    @Test
    void siHayUnaRevisionAbiertaElEntretiempoEsperaAQueSeCierre() {
        crearEIniciar("101");
        irAlMinuto(45);
        EventoPartidoDto gol = gol("home");
        gestor.registrarEvento("101", "VAR_CHECK", null, null, gol.eventId(), null);
        reloj.avanzarMinutos(3);
        gestor.avanzarRelojes();

        assertEquals("EN_REVISION", gestor.listarPartidos().get(0).estado());

        gestor.registrarEvento("101", "MATCH_CONTINUES", null, null, null, null);
        gestor.avanzarRelojes();
        assertEquals("HALF_TIME", ultimoEvento().eventType());
    }

    @Test
    void unGolEnElUltimoMinutoQuedaFirmeAlLlegarElEntretiempo() {
        crearEIniciar("101");
        irAlMinuto(45);
        EventoPartidoDto gol = gol("home");
        reloj.avanzarMinutos(1);

        gestor.avanzarRelojes();

        List<EventoPartidoDto> historial = gestor.listarEventos("101");
        EventoPartidoDto confirmacion = historial.get(historial.size() - 2);
        assertEquals("GOAL_CONFIRMED", confirmacion.eventType());
        assertEquals(gol.eventId(), confirmacion.reviewsEventId());
        assertEquals("HALF_TIME", ultimoEvento().eventType());
    }

    @Test
    void alCumplirseElSegundoTiempoElPartidoTerminaYNoAceptaMasEventos() {
        crearEIniciar("101");
        irAlMinuto(46);
        gestor.iniciarSegundoTiempo("101");
        reloj.avanzarMinutos(45);

        gestor.avanzarRelojes();

        assertEquals("MATCH_ENDED", ultimoEvento().eventType());
        assertEquals("FINALIZADO", gestor.listarPartidos().get(0).estado());
        assertThrows(IllegalStateException.class, () -> gol("home"));
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
}
