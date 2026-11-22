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
    void siNoHayGolPendienteEnviaUnGol() {
        generador.generarEvento();

        EventoPartidoDto evento = capturarUltimoEnvio(1);

        assertEquals("GOAL", evento.eventType());
    }

    @Test
    void siHayGolPendienteYElAzarDiceAnularEnviaGolAnulado() {
        generador.generarEvento();
        String idGol = capturarUltimoEnvio(1).eventId();

        when(azar.nextBoolean()).thenReturn(true);
        generador.generarEvento();
        EventoPartidoDto segundoEvento = capturarUltimoEnvio(2);

        assertEquals("GOAL_ANNULLED", segundoEvento.eventType());
        assertEquals(idGol, segundoEvento.annulsEventId());
    }

    @Test
    void siHayGolPendienteYElAzarDiceSeguirEnviaOtroGol() {
        generador.generarEvento();
        String idPrimerGol = capturarUltimoEnvio(1).eventId();

        when(azar.nextBoolean()).thenReturn(false);
        generador.generarEvento();
        EventoPartidoDto segundoEvento = capturarUltimoEnvio(2);

        assertEquals("GOAL", segundoEvento.eventType());
        assertNotEquals(idPrimerGol, segundoEvento.eventId());
    }

    private EventoPartidoDto capturarUltimoEnvio(int vecesEsperadas) {
        ArgumentCaptor<EventoPartidoDto> captor = ArgumentCaptor.forClass(EventoPartidoDto.class);
        verify(kafkaTemplate, Mockito.times(vecesEsperadas)).send(eq("match_events"), anyString(), captor.capture());
        return captor.getValue();
    }
}
