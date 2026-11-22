package com.varenabled.historicalarchiver.kafka;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.historicalarchiver.persistencia.FinalScoreEntity;
import com.varenabled.historicalarchiver.persistencia.FinalScoreJpaRepository;
import com.varenabled.historicalarchiver.persistencia.MatchEventLogJpaRepository;
import com.varenabled.historicalarchiver.persistencia.VideoReviewAuditJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "match_events")
@DirtiesContext
class EscuchaEventosArchivadorTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MatchEventLogJpaRepository eventosRepositorio;

    @Autowired
    private VideoReviewAuditJpaRepository revisionesRepositorio;

    @Autowired
    private FinalScoreJpaRepository marcadorRepositorio;

    @Test
    void unGolAnuladoQuedaRegistradoEnLasTresTablasYElRelojNoSeArchiva() throws InterruptedException {
        Instant momentoDelGol = Instant.now();
        // El avance del reloj no se archiva: por eso al final solo quedan 2 eventos guardados.
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "CLOCK_TICK", null, "evt_r1", null, 9, 0, null, momentoDelGol));
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 10, 0, null, momentoDelGol));
        kafkaTemplate.send("match_events", "123",
                new EventoPartidoDto("123", "FOOTBALL", "GOAL_ANNULLED", null, null, "evt_g1", 12, 0, null, momentoDelGol.plusSeconds(5)));

        esperarHasta(() -> marcadorRepositorio.findById("123")
                .map(marcador -> marcador.getGolesLocal() == 0)
                .orElse(false));

        assertEquals(2, eventosRepositorio.count());
        assertEquals(1, revisionesRepositorio.count());

        FinalScoreEntity marcador = marcadorRepositorio.findById("123").orElseThrow();
        assertEquals(0, marcador.getGolesLocal());
        assertEquals(0, marcador.getGolesVisitante());
    }

    private void esperarHasta(java.util.function.BooleanSupplier condicion) throws InterruptedException {
        for (int intento = 0; intento < 50; intento++) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
    }
}
