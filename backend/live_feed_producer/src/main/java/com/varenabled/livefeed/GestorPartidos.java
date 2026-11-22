package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.RelojPartido;
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
    private static final String TIEMPO_ANADIDO = "ADDED_TIME";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, EstadoPartido> partidos = new ConcurrentHashMap<>();

    public GestorPartidos(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void crearPartido(String matchId) {
        partidos.putIfAbsent(matchId, new EstadoPartido());
    }

    public List<String> listarPartidos() {
        return List.copyOf(partidos.keySet());
    }

    public List<EventoPartidoDto> listarEventos(String matchId) {
        return List.copyOf(obtenerEstado(matchId).historial);
    }

    public EventoPartidoDto registrarEvento(String matchId, String eventType, String team, String annulsEventId,
                                             int minuto, Integer minutoExtra, Integer minutosAgregados) {
        EstadoPartido estado = obtenerEstado(matchId);
        int extra = minutoExtra == null ? 0 : minutoExtra;

        if (TIEMPO_ANADIDO.equals(eventType)) {
            if (minutosAgregados == null) {
                throw new IllegalArgumentException("minutosAgregados es obligatorio para " + TIEMPO_ANADIDO);
            }
            estado.reloj.anunciarTiempoAnadido(minuto, minutosAgregados);
        } else {
            estado.reloj.validarMinuto(minuto, extra);
        }

        String idEvento = idUnico(eventType);
        EventoPartidoDto evento = new EventoPartidoDto(
                matchId, "FOOTBALL", eventType, team, idEvento, annulsEventId, minuto, extra, minutosAgregados, Instant.now());

        kafkaTemplate.send(TOPICO, matchId, evento);
        estado.historial.add(evento);
        return evento;
    }

    private EstadoPartido obtenerEstado(String matchId) {
        EstadoPartido estado = partidos.get(matchId);
        if (estado == null) {
            throw new PartidoNoEncontradoException(matchId);
        }
        return estado;
    }

    private String idUnico(String eventType) {
        String prefijo = switch (eventType == null ? "" : eventType) {
            case "GOAL" -> "g";
            case "FOUL" -> "f";
            case "VAR_CHECK" -> "v";
            case "GOAL_ANNULLED" -> "a";
            case "MATCH_CONTINUES" -> "c";
            case TIEMPO_ANADIDO -> "t";
            default -> "x";
        };
        return "evt_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static final class EstadoPartido {
        private final List<EventoPartidoDto> historial = new CopyOnWriteArrayList<>();
        private final RelojPartido reloj = new RelojPartido();
    }
}
