package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
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
                () -> gestor.registrarEvento("999", "GOAL", "home", null, null));
    }

    @Test
    void registrarUnGolLoPublicaEnKafkaConElMatchIdComoClave() {
        gestor.crearPartido("101");

        EventoPartidoDto evento = gestor.registrarEvento("101", "GOAL", "home", null, null);

        assertEquals("101", evento.matchId());
        assertEquals("GOAL", evento.eventType());
        assertEquals("home", evento.team());
        assertNotNull(evento.eventId());
        verify(kafkaTemplate).send(eq("match_events"), eq("101"), eq(evento));
    }

    @Test
    void registrarUnaAnulacionConservaElAnnulsEventId() {
        gestor.crearPartido("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);

        EventoPartidoDto anulacion = gestor.registrarEvento("101", "GOAL_ANNULLED", null, gol.eventId(), null);

        assertEquals(gol.eventId(), anulacion.annulsEventId());
    }

    @Test
    void losEventosQuedanEnElHistorialEnOrden() {
        gestor.crearPartido("101");
        EventoPartidoDto gol = gestor.registrarEvento("101", "GOAL", "home", null, null);
        EventoPartidoDto check = gestor.registrarEvento("101", "VAR_CHECK", null, null, null);

        List<EventoPartidoDto> historial = gestor.listarEventos("101");

        assertEquals(List.of(gol, check), historial);
    }

    @Test
    void losEventIdNoSeRepiten() {
        gestor.crearPartido("101");
        EventoPartidoDto primero = gestor.registrarEvento("101", "GOAL", "home", null, null);
        EventoPartidoDto segundo = gestor.registrarEvento("101", "GOAL", "away", null, null);

        assertNotEquals(primero.eventId(), segundo.eventId());
    }
}
