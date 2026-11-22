package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.futbol.MinutoPartido;
import com.varenabled.dominio.futbol.RelojPartido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GestorPartidos {

    private static final String TOPICO = "match_events";
    private static final String TIEMPO_ANADIDO = "ADDED_TIME";
    private static final String CREACION = "MATCH_CREATED";
    private static final String INICIO = "MATCH_STARTED";
    private static final String AVANCE_RELOJ = "CLOCK_TICK";
    private static final String FINAL = "MATCH_ENDED";
    private static final Set<String> EVENTOS_DEL_RELOJ = Set.of(CREACION, INICIO, AVANCE_RELOJ, FINAL);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock reloj;
    private final Duration duracionMinuto;
    private final Map<String, PartidoEnVivo> partidos = new ConcurrentHashMap<>();

    public GestorPartidos(KafkaTemplate<String, Object> kafkaTemplate, Clock reloj,
                          @Value("${partido.segundos-por-minuto:60}") long segundosPorMinuto) {
        this.kafkaTemplate = kafkaTemplate;
        this.reloj = reloj;
        this.duracionMinuto = Duration.ofSeconds(segundosPorMinuto);
    }

    /** Avisa por Kafka que el partido existe (sin minuto todavia), para que el dashboard lo muestre "por jugar". */
    public void crearPartido(String matchId) {
        PartidoEnVivo nuevo = new PartidoEnVivo();
        if (partidos.putIfAbsent(matchId, nuevo) != null) {
            return;
        }
        synchronized (nuevo) {
            EventoPartidoDto creacion = new EventoPartidoDto(matchId, "FOOTBALL", CREACION, null, idUnico(CREACION),
                    null, null, null, null, reloj.instant());
            kafkaTemplate.send(TOPICO, matchId, creacion);
            nuevo.historial.add(creacion);
        }
    }

    public List<ResumenPartido> listarPartidos() {
        Instant ahora = reloj.instant();
        List<ResumenPartido> resumen = new ArrayList<>();
        partidos.forEach((matchId, partido) -> resumen.add(partido.resumir(matchId, ahora)));
        return resumen;
    }

    public List<EventoPartidoDto> listarEventos(String matchId) {
        return List.copyOf(obtenerPartido(matchId).historial);
    }

    public EventoPartidoDto iniciarPartido(String matchId) {
        PartidoEnVivo partido = obtenerPartido(matchId);
        synchronized (partido) {
            if (partido.reloj != null) {
                throw new IllegalStateException("El partido " + matchId + " ya empezó");
            }
            partido.reloj = new RelojPartido(reloj.instant(), duracionMinuto);
            partido.ultimoMinuto = new MinutoPartido(1, 0, false);
            EventoPartidoDto inicio = publicar(matchId, INICIO, null, null, partido.ultimoMinuto, null);
            partido.historial.add(inicio);
            return inicio;
        }
    }

    /** El minuto no se recibe: se toma del reloj del partido en el momento en que llega el evento. */
    public EventoPartidoDto registrarEvento(String matchId, String eventType, String team, String annulsEventId,
                                             Integer minutosAgregados) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("El event_type es obligatorio");
        }
        if (EVENTOS_DEL_RELOJ.contains(eventType)) {
            throw new IllegalArgumentException(eventType + " lo maneja el reloj del partido, no se envía a mano");
        }

        PartidoEnVivo partido = obtenerPartido(matchId);
        synchronized (partido) {
            Instant ahora = reloj.instant();
            if (partido.reloj == null) {
                throw new IllegalStateException("El partido " + matchId + " todavía no empezó");
            }
            if (partido.terminado || partido.reloj.minutoEn(ahora).finalizado()) {
                throw new IllegalStateException("El partido " + matchId + " ya terminó");
            }

            if (TIEMPO_ANADIDO.equals(eventType)) {
                if (minutosAgregados == null) {
                    throw new IllegalArgumentException("minutos_agregados es obligatorio para " + TIEMPO_ANADIDO);
                }
                partido.reloj.anunciarTiempoAnadido(ahora, minutosAgregados);
            }

            MinutoPartido minuto = partido.reloj.minutoEn(ahora);
            EventoPartidoDto evento = publicar(matchId, eventType, team, annulsEventId, minuto,
                    TIEMPO_ANADIDO.equals(eventType) ? minutosAgregados : null);
            partido.historial.add(evento);
            partido.ultimoMinuto = minuto;
            return evento;
        }
    }

    /**
     * Se llama cada segundo: si el minuto de un partido cambio, avisa por Kafka (CLOCK_TICK)
     * para que el marcador avance aunque no pase nada; si se cumplio el tiempo, lo termina (MATCH_ENDED).
     */
    public void avanzarRelojes() {
        Instant ahora = reloj.instant();
        partidos.forEach((matchId, partido) -> {
            synchronized (partido) {
                if (partido.reloj == null || partido.terminado) {
                    return;
                }
                MinutoPartido minuto = partido.reloj.minutoEn(ahora);
                if (minuto.finalizado()) {
                    partido.terminado = true;
                    partido.historial.add(publicar(matchId, FINAL, null, null, minuto, null));
                } else if (!minuto.equals(partido.ultimoMinuto)) {
                    partido.ultimoMinuto = minuto;
                    publicar(matchId, AVANCE_RELOJ, null, null, minuto, null);
                }
            }
        });
    }

    private EventoPartidoDto publicar(String matchId, String eventType, String team, String annulsEventId,
                                      MinutoPartido minuto, Integer minutosAgregados) {
        EventoPartidoDto evento = new EventoPartidoDto(matchId, "FOOTBALL", eventType, team, idUnico(eventType),
                annulsEventId, minuto.minuto(), minuto.minutoExtra(), minutosAgregados, reloj.instant());
        kafkaTemplate.send(TOPICO, matchId, evento);
        return evento;
    }

    private PartidoEnVivo obtenerPartido(String matchId) {
        PartidoEnVivo partido = partidos.get(matchId);
        if (partido == null) {
            throw new PartidoNoEncontradoException(matchId);
        }
        return partido;
    }

    private String idUnico(String eventType) {
        String prefijo = switch (eventType) {
            case "GOAL" -> "g";
            case "FOUL" -> "f";
            case "VAR_CHECK" -> "v";
            case "GOAL_ANNULLED" -> "a";
            case "MATCH_CONTINUES" -> "c";
            case TIEMPO_ANADIDO -> "t";
            case CREACION -> "n";
            case INICIO -> "i";
            case AVANCE_RELOJ -> "r";
            case FINAL -> "fin";
            default -> "x";
        };
        return "evt_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static final class PartidoEnVivo {
        private final List<EventoPartidoDto> historial = new CopyOnWriteArrayList<>();
        private RelojPartido reloj;
        private MinutoPartido ultimoMinuto;
        private boolean terminado;

        private synchronized ResumenPartido resumir(String matchId, Instant ahora) {
            if (reloj == null) {
                return new ResumenPartido(matchId, "POR_JUGAR", null, null);
            }
            MinutoPartido minuto = reloj.minutoEn(ahora);
            String estado = terminado || minuto.finalizado() ? "FINALIZADO" : "EN_JUEGO";
            return new ResumenPartido(matchId, estado, minuto.minuto(), minuto.minutoExtra());
        }
    }
}
