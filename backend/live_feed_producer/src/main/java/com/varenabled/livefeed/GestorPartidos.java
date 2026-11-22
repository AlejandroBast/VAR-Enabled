package com.varenabled.livefeed;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.futbol.ControlVar;
import com.varenabled.dominio.futbol.ControlVar.TipoJugada;
import com.varenabled.dominio.futbol.FasePartido;
import com.varenabled.dominio.futbol.MinutoPartido;
import com.varenabled.dominio.futbol.ReglaPartidoException;
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

    // Eventos que se mandan a mano desde la API.
    private static final String GOL = "GOAL";
    private static final String FALTA = "FOUL";
    private static final String AMARILLA = "YELLOW_CARD";
    private static final String ROJA = "RED_CARD";
    private static final String REVISION = "VAR_CHECK";
    private static final String CONTINUA = "MATCH_CONTINUES";
    private static final String GOL_ANULADO = "GOAL_ANNULLED";
    private static final String FALTA_ANULADA = "FOUL_ANNULLED";
    private static final String TIEMPO_ANADIDO = "ADDED_TIME";
    private static final Set<String> EVENTOS_MANUALES = Set.of(
            GOL, FALTA, AMARILLA, ROJA, REVISION, CONTINUA, GOL_ANULADO, FALTA_ANULADA, TIEMPO_ANADIDO);
    private static final Set<String> CON_EQUIPO = Set.of(GOL, FALTA, AMARILLA, ROJA);
    private static final Set<String> CIERRAN_REVISION = Set.of(CONTINUA, GOL_ANULADO, FALTA_ANULADA);

    // Eventos que manda el propio sistema.
    private static final String CREACION = "MATCH_CREATED";
    private static final String INICIO = "MATCH_STARTED";
    private static final String AVANCE_RELOJ = "CLOCK_TICK";
    private static final String GOL_FIRME = "GOAL_CONFIRMED";
    private static final String ENTRETIEMPO = "HALF_TIME";
    private static final String SEGUNDO_TIEMPO = "SECOND_HALF_STARTED";
    private static final String FINAL = "MATCH_ENDED";

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
    public void crearPartido(String matchId, String equipoLocal, String equipoVisitante) {
        PartidoEnVivo nuevo = new PartidoEnVivo(textoONulo(equipoLocal), textoONulo(equipoVisitante));
        if (partidos.putIfAbsent(matchId, nuevo) != null) {
            return;
        }
        synchronized (nuevo) {
            EventoPartidoDto creacion = new EventoPartidoDto(matchId, "FOOTBALL", CREACION, null, idUnico(CREACION),
                    null, null, null, null, reloj.instant(), null, nuevo.equipoLocal, nuevo.equipoVisitante);
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
            partido.ultimoMinuto = partido.reloj.minutoEn(reloj.instant());
            return publicarYGuardar(matchId, partido, INICIO, null, null, null, partido.ultimoMinuto, null);
        }
    }

    public EventoPartidoDto iniciarSegundoTiempo(String matchId) {
        PartidoEnVivo partido = obtenerPartido(matchId);
        synchronized (partido) {
            if (!partido.enEntretiempo) {
                throw new IllegalStateException("El partido " + matchId + " no está en el entretiempo");
            }
            Instant ahora = reloj.instant();
            partido.reloj.iniciarSegundoTiempo(ahora);
            partido.enEntretiempo = false;
            partido.ultimoMinuto = partido.reloj.minutoEn(ahora);
            return publicarYGuardar(matchId, partido, SEGUNDO_TIEMPO, null, null, null, partido.ultimoMinuto, null);
        }
    }

    /** El minuto no se recibe: se toma del reloj del partido en el momento en que llega el evento. */
    public EventoPartidoDto registrarEvento(String matchId, String eventType, String team, String annulsEventId,
                                             String reviewsEventId, Integer minutosAgregados) {
        if (eventType == null || !EVENTOS_MANUALES.contains(eventType)) {
            throw new IllegalArgumentException("event_type no válido: " + eventType
                    + ". Se puede mandar: " + String.join(", ", EVENTOS_MANUALES));
        }
        if (CON_EQUIPO.contains(eventType) && !"home".equals(team) && !"away".equals(team)) {
            throw new IllegalArgumentException(eventType + " necesita team: home o away");
        }

        PartidoEnVivo partido = obtenerPartido(matchId);
        synchronized (partido) {
            Instant ahora = reloj.instant();
            if (partido.reloj == null) {
                throw new IllegalStateException("El partido " + matchId + " todavía no empezó");
            }
            if (partido.terminado) {
                throw new IllegalStateException("El partido " + matchId + " ya terminó");
            }
            MinutoPartido minuto = partido.reloj.minutoEn(ahora);

            // Cerrar una revision siempre se puede (aunque el tiempo se haya cumplido mientras el VAR revisaba).
            if (!CIERRAN_REVISION.contains(eventType)) {
                if (!minuto.fase().seEstaJugando()) {
                    throw new IllegalStateException("El partido " + matchId + " no se está jugando ahora ("
                            + (minuto.fase() == FasePartido.ENTRETIEMPO ? "entretiempo" : "terminó") + ")");
                }
                if (partido.var.hayRevisionAbierta()) {
                    throw new ReglaPartidoException("El juego está parado por una revisión VAR: primero hay que cerrarla");
                }
            }

            String idEvento = idUnico(eventType);
            String revisado = null;
            String anulado = null;
            switch (eventType) {
                case GOL -> partido.var.registrarJugada(new IdEvento(idEvento), TipoJugada.GOL, minuto.minutosJugados());
                case FALTA -> partido.var.registrarJugada(new IdEvento(idEvento), TipoJugada.FALTA, minuto.minutosJugados());
                case REVISION -> {
                    if (reviewsEventId == null || reviewsEventId.isBlank()) {
                        throw new IllegalArgumentException("VAR_CHECK necesita reviews_event_id: el gol o la falta a revisar");
                    }
                    partido.var.abrirRevision(new IdEvento(reviewsEventId), minuto.minutosJugados());
                    revisado = reviewsEventId;
                }
                case CONTINUA -> revisado = partido.var.confirmar().valor();
                case GOL_ANULADO, FALTA_ANULADA -> {
                    // Si no se indica, se anula la jugada que esta en revision.
                    IdEvento enRevision = partido.var.jugadaEnRevision();
                    anulado = annulsEventId != null && !annulsEventId.isBlank() ? annulsEventId
                            : enRevision == null ? null : enRevision.valor();
                    if (anulado == null) {
                        throw new ReglaPartidoException("Solo se puede anular una jugada que esté en revisión VAR");
                    }
                    partido.var.anular(new IdEvento(anulado), GOL_ANULADO.equals(eventType) ? TipoJugada.GOL : TipoJugada.FALTA);
                }
                case TIEMPO_ANADIDO -> {
                    if (minutosAgregados == null) {
                        throw new IllegalArgumentException("minutos_agregados es obligatorio para " + TIEMPO_ANADIDO);
                    }
                    partido.reloj.anunciarTiempoAnadido(ahora, minutosAgregados);
                    minuto = partido.reloj.minutoEn(ahora);
                }
                default -> {
                    // Tarjetas: no se revisan, solo se registran.
                }
            }

            EventoPartidoDto evento = new EventoPartidoDto(matchId, "FOOTBALL", eventType,
                    CON_EQUIPO.contains(eventType) ? team : null, idEvento, anulado, minuto.minuto(), minuto.minutoExtra(),
                    TIEMPO_ANADIDO.equals(eventType) ? minutosAgregados : null, ahora, revisado, null, null);
            kafkaTemplate.send(TOPICO, matchId, evento);
            partido.historial.add(evento);
            return evento;
        }
    }

    /**
     * Se llama cada segundo. Por cada partido en juego:
     * - los goles que pasaron sus 2 minutos sin revision quedan firmes (GOAL_CONFIRMED),
     * - si cambio el minuto, avisa (CLOCK_TICK) para que el marcador avance aunque no pase nada,
     * - al cumplirse el tiempo pasa al entretiempo (HALF_TIME) o termina (MATCH_ENDED).
     * Si hay una revision VAR abierta, el entretiempo o el final esperan a que se cierre.
     */
    public void avanzarRelojes() {
        Instant ahora = reloj.instant();
        partidos.forEach((matchId, partido) -> {
            synchronized (partido) {
                if (partido.reloj == null || partido.terminado || partido.enEntretiempo) {
                    return;
                }
                MinutoPartido minuto = partido.reloj.minutoEn(ahora);

                if (minuto.fase().seEstaJugando()) {
                    confirmarGoles(matchId, partido, partido.var.cerrarVentanasVencidas(minuto.minutosJugados()), minuto);
                    if (!minuto.equals(partido.ultimoMinuto)) {
                        partido.ultimoMinuto = minuto;
                        publicar(matchId, AVANCE_RELOJ, null, null, null, minuto, null);
                    }
                    return;
                }

                if (partido.var.hayRevisionAbierta()) {
                    return;
                }
                confirmarGoles(matchId, partido, partido.var.cerrarTodasLasVentanas(), minuto);
                partido.ultimoMinuto = minuto;
                if (minuto.fase() == FasePartido.ENTRETIEMPO) {
                    partido.enEntretiempo = true;
                    publicarYGuardar(matchId, partido, ENTRETIEMPO, null, null, null, minuto, null);
                } else {
                    partido.terminado = true;
                    publicarYGuardar(matchId, partido, FINAL, null, null, null, minuto, null);
                }
            }
        });
    }

    private void confirmarGoles(String matchId, PartidoEnVivo partido, List<IdEvento> golesFirmes, MinutoPartido minuto) {
        for (IdEvento gol : golesFirmes) {
            publicarYGuardar(matchId, partido, GOL_FIRME, null, null, gol.valor(), minuto, null);
        }
    }

    private EventoPartidoDto publicarYGuardar(String matchId, PartidoEnVivo partido, String eventType, String team,
                                              String annulsEventId, String reviewsEventId, MinutoPartido minuto,
                                              Integer minutosAgregados) {
        EventoPartidoDto evento = publicar(matchId, eventType, team, annulsEventId, reviewsEventId, minuto, minutosAgregados);
        partido.historial.add(evento);
        return evento;
    }

    private EventoPartidoDto publicar(String matchId, String eventType, String team, String annulsEventId,
                                      String reviewsEventId, MinutoPartido minuto, Integer minutosAgregados) {
        EventoPartidoDto evento = new EventoPartidoDto(matchId, "FOOTBALL", eventType, team, idUnico(eventType),
                annulsEventId, minuto.minuto(), minuto.minutoExtra(), minutosAgregados, reloj.instant(),
                reviewsEventId, null, null);
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

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String idUnico(String eventType) {
        String prefijo = switch (eventType) {
            case GOL -> "g";
            case FALTA -> "f";
            case AMARILLA -> "ta";
            case ROJA -> "tr";
            case REVISION -> "v";
            case GOL_ANULADO -> "a";
            case FALTA_ANULADA -> "fa";
            case CONTINUA -> "c";
            case TIEMPO_ANADIDO -> "t";
            case CREACION -> "n";
            case INICIO -> "i";
            case AVANCE_RELOJ -> "r";
            case GOL_FIRME -> "gf";
            case ENTRETIEMPO -> "et";
            case SEGUNDO_TIEMPO -> "st";
            case FINAL -> "fin";
            default -> "x";
        };
        return "evt_" + prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static final class PartidoEnVivo {
        private final String equipoLocal;
        private final String equipoVisitante;
        private final List<EventoPartidoDto> historial = new CopyOnWriteArrayList<>();
        private final ControlVar var = new ControlVar();
        private RelojPartido reloj;
        private MinutoPartido ultimoMinuto;
        private boolean enEntretiempo;
        private boolean terminado;

        private PartidoEnVivo(String equipoLocal, String equipoVisitante) {
            this.equipoLocal = equipoLocal;
            this.equipoVisitante = equipoVisitante;
        }

        private synchronized ResumenPartido resumir(String matchId, Instant ahora) {
            if (reloj == null) {
                return new ResumenPartido(matchId, equipoLocal, equipoVisitante, "POR_JUGAR", null, null, null, List.of());
            }
            MinutoPartido minuto = reloj.minutoEn(ahora);
            String estado;
            if (terminado) {
                estado = "FINALIZADO";
            } else if (var.hayRevisionAbierta()) {
                estado = "EN_REVISION";
            } else if (enEntretiempo) {
                estado = "ENTRETIEMPO";
            } else {
                estado = "EN_JUEGO";
            }
            IdEvento enRevision = var.jugadaEnRevision();
            List<String> revisables = var.jugadasRevisables(minuto.minutosJugados()).stream().map(IdEvento::valor).toList();
            return new ResumenPartido(matchId, equipoLocal, equipoVisitante, estado, minuto.minuto(), minuto.minutoExtra(),
                    enRevision == null ? null : enRevision.valor(), revisables);
        }
    }
}
