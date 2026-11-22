package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GestorPartidosTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private GestorPartidos gestor;

    @BeforeEach
    void preparar() {
        gestor = new GestorPartidos(kafkaTemplate);
    }

    @Test
    void unPartidoCreadoApareceEnElListado() {
        gestor.crearPartido("101");

        assertTrue(gestor.listarPartidos().contains("101"));
    }

    @Test
    void crearElMismoPartidoDosVecesNoLoDuplica() {
        gestor.crearPartido("101");
        gestor.crearPartido("101");

        assertEquals(1, gestor.listarPartidos().size());
    }

    @Test
    void registrarUnEventoParaUnPartidoNoCreadoLanzaExcepcion() {
        assertThrows(PartidoNoEncontradoException.class,
                () -> gestor.registrarEvento("999", "GOAL", "home", null, 10, null, null));
    }

    @Test
    void registrarUnGolLoPublicaEnKafkaConElMatchIdComoClave() {
        gestor.crearPartido("101");

        EventoPartidoDto evento = gestor.registrarEvento("101", "GOAL", "home", null, 23, null, null);

        assertEquals("101", evento.matchId());
        assertEquals("GOAL", evento.eventType());
        assertEquals("home", evento.team());
        assertEquals(23, evento.minuto());
        assertEquals(0, evento.minutoExtra());
        assertNotNull(evento.eventId());
        verify(kafkaTemplate).send(eq("match_events"), eq("101"), eq(evento));
    }

    @Test
    void registrarUnaAnulacionConservaElAnnulsEventId() {
        gestor.crearPartido("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, 10, null, null);

        EventoPartidoDto anulacion = gestor.registrarEvento("101", "GOAL_ANNULLED", null, gol.eventId(), 12, null, null);

        assertEquals(gol.eventId(), anulacion.annulsEventId());
    }

    @Test
    void losEventosQuedanEnElHistorialEnOrden() {
        gestor.crearPartido("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, 10, null, null);
        EventoPartidoDto check = gestor.registrarEvento("101", "VAR_CHECK", null, null, 11, null, null);

        List<EventoPartidoDto> historial = gestor.listarEventos("101");

        assertEquals(List.of(gol, check), historial);
    }

    @Test
    void losEventIdNoSeRepiten() {
        gestor.crearPartido("101");
        EventoPartidoDto primero = gestor.registrarEvento("101", "GOAL", "home", null, 10, null, null);
        EventoPartidoDto segundo = gestor.registrarEvento("101", "GOAL", "away", null, 11, null, null);

        assertNotEquals(primero.eventId(), segundo.eventId());
    }

    @Test
    void unMinutoFueraDeLos90ReglamentariosLanzaMinutoInvalido() {
        gestor.crearPartido("101");

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("101", "GOAL", "home", null, 91, null, null));
    }

    @Test
    void elTiempoAnadidoSoloSePuedeAnunciarEnElCierreDeUnaMitad() {
        gestor.crearPartido("101");

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("101", "ADDED_TIME", null, null, 30, null, 3));
    }

    @Test
    void anunciarElTiempoAnadidoSinMinutosAgregadosLanzaExcepcion() {
        gestor.crearPartido("101");

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarEvento("101", "ADDED_TIME", null, null, 45, null, null));
    }

    @Test
    void despuesDeAnunciarTiempoAnadidoSePuedenRegistrarEventosDentroDeEseRango() {
        gestor.crearPartido("101");
        gestor.registrarEvento("101", "ADDED_TIME", null, null, 45, null, 3);

        EventoPartidoDto evento = gestor.registrarEvento("101", "GOAL", "home", null, 45, 2, null);

        assertEquals(45, evento.minuto());
        assertEquals(2, evento.minutoExtra());
    }

    @Test
    void sinAnunciarTiempoAnadidoUnMinutoExtraEsRechazado() {
        gestor.crearPartido("101");

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("101", "GOAL", "home", null, 45, 1, null));
    }

    @Test
    void elTiempoAnadidoDeUnPartidoNoAfectaAOtro() {
        gestor.crearPartido("101");
        gestor.crearPartido("202");
        gestor.registrarEvento("101", "ADDED_TIME", null, null, 45, null, 5);

        assertThrows(MinutoInvalidoException.class,
                () -> gestor.registrarEvento("202", "GOAL", "home", null, 45, 1, null));
    }
}
