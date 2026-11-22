package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GeneradorEventosFutbol {

    private static final List<String> PARTIDOS = List.of("123", "456", "789");
    private static final String TOPICO = "match_events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random azar;
    private final Map<String, EstadoPartido> estados = new ConcurrentHashMap<>();

    @Autowired
    public GeneradorEventosFutbol(KafkaTemplate<String, Object> kafkaTemplate) {
        this(kafkaTemplate, new Random());
    }

    GeneradorEventosFutbol(KafkaTemplate<String, Object> kafkaTemplate, Random azar) {
        this.kafkaTemplate = kafkaTemplate;
        this.azar = azar;
    }

    @Scheduled(fixedDelay = 10000)
    public void generarEventos() {
        PARTIDOS.forEach(this::avanzar);
    }

    private void avanzar(String idPartido) {
        EstadoPartido estado = estados.computeIfAbsent(idPartido, id -> new EstadoPartido());

        if (estado.idGolEnRevision == null) {
            if (azar.nextInt(4) == 0) {
                enviarFalta(idPartido);
            } else {
                enviarGol(idPartido, estado);
            }
        } else if (!estado.varConsultado) {
            enviarVarCheck(idPartido, estado);
        } else if (azar.nextBoolean()) {
            enviarGolAnulado(idPartido, estado);
        } else {
            enviarSigueElJuego(idPartido, estado);
        }
    }

    private void enviarGol(String idPartido, EstadoPartido estado) {
        String equipo = azar.nextBoolean() ? "home" : "away";
        String idEvento = idUnico("g");
        EventoPartidoDto evento = new EventoPartidoDto(idPartido, "FOOTBALL", "GOAL", equipo, idEvento, null, null, Instant.now());

        kafkaTemplate.send(TOPICO, idPartido, evento);
        estado.idGolEnRevision = idEvento;
        estado.varConsultado = false;
    }

    private void enviarFalta(String idPartido) {
        EventoPartidoDto evento = new EventoPartidoDto(idPartido, "FOOTBALL", "FOUL", null, idUnico("f"), null, null, Instant.now());

        kafkaTemplate.send(TOPICO, idPartido, evento);
    }

    private void enviarVarCheck(String idPartido, EstadoPartido estado) {
        EventoPartidoDto evento = new EventoPartidoDto(idPartido, "FOOTBALL", "VAR_CHECK", null, idUnico("v"), null, null, Instant.now());

        kafkaTemplate.send(TOPICO, idPartido, evento);
        estado.varConsultado = true;
    }

    private void enviarGolAnulado(String idPartido, EstadoPartido estado) {
        EventoPartidoDto evento = new EventoPartidoDto(idPartido, "FOOTBALL", "GOAL_ANNULLED", null, idUnico("a"), estado.idGolEnRevision, null, Instant.now());

        kafkaTemplate.send(TOPICO, idPartido, evento);
        estado.idGolEnRevision = null;
        estado.varConsultado = false;
    }

    private void enviarSigueElJuego(String idPartido, EstadoPartido estado) {
        EventoPartidoDto evento = new EventoPartidoDto(idPartido, "FOOTBALL", "MATCH_CONTINUES", null, idUnico("c"), null, null, Instant.now());

        kafkaTemplate.send(TOPICO, idPartido, evento);
        estado.idGolEnRevision = null;
        estado.varConsultado = false;
    }

    private String idUnico(String prefijo) {
        return "evt_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static final class EstadoPartido {
        private volatile String idGolEnRevision;
        private volatile boolean varConsultado;
    }
}
