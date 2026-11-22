package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GestorPartidos {

    private static final String TOPICO = "match_events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, List<EventoPartidoDto>> partidos = new ConcurrentHashMap<>();

    public GestorPartidos(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void crearPartido(String matchId) {
        partidos.putIfAbsent(matchId, new CopyOnWriteArrayList<>());
    }

    public List<String> listarPartidos() {
        return List.copyOf(partidos.keySet());
    }

    public List<EventoPartidoDto> listarEventos(String matchId) {
        return List.copyOf(obtenerHistorial(matchId));
    }

    public EventoPartidoDto registrarEvento(String matchId, String eventType, String team, String annulsEventId, Integer points) {
        List<EventoPartidoDto> historial = obtenerHistorial(matchId);

        String idEvento = idUnico(eventType);
        EventoPartidoDto evento = new EventoPartidoDto(matchId, "FOOTBALL", eventType, team, idEvento, annulsEventId, points, Instant.now());

        kafkaTemplate.send(TOPICO, matchId, evento);
        historial.add(evento);
        return evento;
    }

    private List<EventoPartidoDto> obtenerHistorial(String matchId) {
        List<EventoPartidoDto> historial = partidos.get(matchId);
        if (historial == null) {
            throw new PartidoNoEncontradoException(matchId);
        }
        return historial;
    }

    private String idUnico(String eventType) {
        String prefijo = switch (eventType == null ? "" : eventType) {
            case "GOAL" -> "g";
            case "FOUL" -> "f";
            case "VAR_CHECK" -> "v";
            case "GOAL_ANNULLED" -> "a";
            case "MATCH_CONTINUES" -> "c";
            default -> "x";
        };
        return "evt_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
