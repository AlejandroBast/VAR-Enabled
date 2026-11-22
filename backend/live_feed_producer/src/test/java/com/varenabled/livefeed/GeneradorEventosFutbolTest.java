package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneradorEventosFutbolTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Random azar;

    private GeneradorEventosFutbol generador;

    @BeforeEach
    void preparar() {
        generador = new GeneradorEventosFutbol(kafkaTemplate, azar);
    }

    @Test
    void siNoHayGolPendienteYElAzarNoDiceFaltaEnviaGol() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();

        EventoPartidoDto evento = eventosDe("123").get(0);
        assertEquals("GOAL", evento.eventType());
        assertEquals("home", evento.team());
    }

    @Test
    void siNoHayGolPendienteYElAzarDiceFaltaEnviaFalta() {
        when(azar.nextInt(4)).thenReturn(0);

        generador.generarEventos();

        EventoPartidoDto evento = eventosDe("123").get(0);
        assertEquals("FOUL", evento.eventType());
    }

    @Test
    void despuesDeUnGolElSiguienteEventoDeEsePartidoEsVarCheck() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();
        generador.generarEventos();

        List<EventoPartidoDto> eventos = eventosDe("123");
        assertEquals(2, eventos.size());
        assertEquals("VAR_CHECK", eventos.get(1).eventType());
    }

    @Test
    void despuesDeVarCheckSaleGolAnuladoConElIdDelGolOriginal() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();
        generador.generarEventos();
        generador.generarEventos();

        List<EventoPartidoDto> eventos = eventosDe("123");
        assertEquals("GOAL_ANNULLED", eventos.get(2).eventType());
        assertEquals(eventos.get(0).eventId(), eventos.get(2).annulsEventId());
    }

    @Test
    void siElAzarDiceQueSigueElJuegoNoHayAnulacionYSeLimpiaElEstado() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(false);

        generador.generarEventos();
        generador.generarEventos();
        generador.generarEventos();
        generador.generarEventos();

        List<EventoPartidoDto> eventos = eventosDe("123");
        assertEquals("MATCH_CONTINUES", eventos.get(2).eventType());
        assertEquals("GOAL", eventos.get(3).eventType());
    }

    @Test
    void cadaEventoSeEnviaConElMatchIdComoClaveDeKafka() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventoPartidoDto> eventos = ArgumentCaptor.forClass(EventoPartidoDto.class);
        verify(kafkaTemplate, Mockito.times(3)).send(eq("match_events"), claves.capture(), eventos.capture());

        for (int i = 0; i < claves.getAllValues().size(); i++) {
            assertEquals(eventos.getAllValues().get(i).matchId(), claves.getAllValues().get(i));
        }
    }

    @Test
    void losEventIdNoSeRepiten() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();
        generador.generarEventos();

        List<String> ids = todosLosEnviados().stream().map(EventoPartidoDto::eventId).toList();
        assertEquals(ids.size(), new HashSet<>(ids).size());
    }

    @Test
    void losPartidosNoSeMezclanLaAnulacionUsaElGolDeSuPropioPartido() {
        when(azar.nextInt(4)).thenReturn(1);
        when(azar.nextBoolean()).thenReturn(true);

        generador.generarEventos();
        generador.generarEventos();
        generador.generarEventos();

        String golDe123 = eventosDe("123").get(0).eventId();
        String golDe456 = eventosDe("456").get(0).eventId();
        assertNotEquals(golDe123, golDe456);

        assertEquals(golDe123, eventosDe("123").get(2).annulsEventId());
        assertEquals(golDe456, eventosDe("456").get(2).annulsEventId());
    }

    private List<EventoPartidoDto> todosLosEnviados() {
        ArgumentCaptor<EventoPartidoDto> captor = ArgumentCaptor.forClass(EventoPartidoDto.class);
        verify(kafkaTemplate, Mockito.atLeastOnce()).send(eq("match_events"), anyString(), captor.capture());
        return captor.getAllValues();
    }

    private List<EventoPartidoDto> eventosDe(String matchId) {
        return todosLosEnviados().stream().filter(e -> e.matchId().equals(matchId)).toList();
    }
}
