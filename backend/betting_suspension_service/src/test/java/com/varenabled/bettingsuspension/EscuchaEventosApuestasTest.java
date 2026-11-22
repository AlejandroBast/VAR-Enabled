package com.varenabled.bettingsuspension;

import com.varenabled.contratos.ComandoApuestaDto;
import com.varenabled.contratos.EventoPartidoDto;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "match_events")
@DirtiesContext
class EscuchaEventosApuestasTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void unGolOrdenaSuspenderLasApuestas() {
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 10, 0, null, Instant.now()));

        verify(rabbitTemplate, timeout(5000)).convertAndSend(
                eq(ConfiguracionRabbitMQ.COLA_COMANDOS),
                eq(new ComandoApuestaDto("123", "SUSPEND_BETS")));
    }

    @Test
    void unGolAnuladoOrdenaReanudarLasApuestas() {
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "GOAL_ANNULLED", null, null, "evt_g1", 12, 0, null, Instant.now()));

        verify(rabbitTemplate, timeout(5000)).convertAndSend(
                eq(ConfiguracionRabbitMQ.COLA_COMANDOS),
                eq(new ComandoApuestaDto("123", "RESUME_BETS")));
    }

    @Test
    void unEventoQueNoAfectaLasApuestasNoPublicaNada() throws InterruptedException {
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "MATCH_STARTED", null, null, null, 1, 0, null, Instant.now()));

        Thread.sleep(1000);
        verifyNoInteractions(rabbitTemplate);
    }
}
