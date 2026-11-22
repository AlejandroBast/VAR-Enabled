package com.varenabled.matchstate.kafka;

import com.varenabled.aplicacion.futbol.ServicioMarcadorFutbol;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscuchaEventosPartidoSeekTest {

    @Mock
    private ServicioMarcadorFutbol servicio;

    @Mock
    private ConsumerSeekAware.ConsumerSeekCallback callback;

    @Test
    void alAsignarseLasParticionesReleeTodoElTopicoDesdeElPrincipio() {
        EscuchaEventosPartido escucha = new EscuchaEventosPartido(servicio);
        Map<TopicPartition, Long> asignaciones = Map.of(new TopicPartition("match_events", 0), 42L);

        escucha.onPartitionsAssigned(asignaciones, callback);

        verify(callback).seekToBeginning("match_events", 0);
    }
}
