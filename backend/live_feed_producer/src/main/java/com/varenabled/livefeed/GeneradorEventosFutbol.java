package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GeneradorEventosFutbol {

    private static final String ID_PARTIDO = "123";
    private static final String TOPICO = "match_events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random azar;
    private final AtomicInteger contadorDeGoles = new AtomicInteger();
    private volatile String idUltimoGolSinAnular;

    @Autowired
    public GeneradorEventosFutbol(KafkaTemplate<String, Object> kafkaTemplate) {
        this(kafkaTemplate, new Random());
    }

    GeneradorEventosFutbol(KafkaTemplate<String, Object> kafkaTemplate, Random azar) {
        this.kafkaTemplate = kafkaTemplate;
        this.azar = azar;
    }

    @Scheduled(fixedDelay = 10000)
    public void generarEvento() {
        if (idUltimoGolSinAnular != null && azar.nextBoolean()) {
            enviarGolAnulado();
        } else {
            enviarGol();
        }
    }

    private void enviarGol() {
        String idEvento = "evt_g" + contadorDeGoles.incrementAndGet();
        EventoPartidoDto evento = new EventoPartidoDto(ID_PARTIDO, "FOOTBALL", "GOAL", "home", idEvento, null, null, Instant.now());

        kafkaTemplate.send(TOPICO, ID_PARTIDO, evento);
        idUltimoGolSinAnular = idEvento;
    }

    private void enviarGolAnulado() {
        EventoPartidoDto evento = new EventoPartidoDto(ID_PARTIDO, "FOOTBALL", "GOAL_ANNULLED", null, null, idUltimoGolSinAnular, null, Instant.now());

        kafkaTemplate.send(TOPICO, ID_PARTIDO, evento);
        idUltimoGolSinAnular = null;
    }
}
