package com.varenabled.matchstate.kafka;

import com.varenabled.aplicacion.futbol.ServicioMarcadorFutbol;
import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EscuchaEventosPartido implements ConsumerSeekAware {

    private final ServicioMarcadorFutbol servicio;

    public EscuchaEventosPartido(ServicioMarcadorFutbol servicio) {
        this.servicio = servicio;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        assignments.keySet().forEach(particion -> callback.seekToBeginning(particion.topic(), particion.partition()));
    }

    @KafkaListener(topics = "match_events", groupId = "match-state-service")
    public void escuchar(EventoPartidoDto evento) {
        IdPartido idPartido = new IdPartido(evento.matchId());
        IdEvento idEvento = id(evento.eventId());
        int minuto = evento.minuto() == null ? 0 : evento.minuto();
        int minutoExtra = evento.minutoExtra() == null ? 0 : evento.minutoExtra();

        switch (evento.eventType()) {
            case "MATCH_CREATED" -> servicio.procesarCreacion(idPartido, evento.homeTeam(), evento.awayTeam());
            case "MATCH_STARTED" -> servicio.procesarInicio(idPartido, idEvento, minuto, minutoExtra);
            case "HALF_TIME" -> servicio.procesarEntretiempo(idPartido, idEvento, minuto, minutoExtra);
            case "SECOND_HALF_STARTED" -> servicio.procesarSegundoTiempo(idPartido, idEvento, minuto, minutoExtra);
            case "MATCH_ENDED" -> servicio.procesarFinal(idPartido, idEvento, minuto, minutoExtra);
            case "GOAL" -> servicio.procesarGol(idPartido, idEvento, mapearEquipo(evento.team()), minuto, minutoExtra);
            case "GOAL_ANNULLED" -> servicio.procesarGolAnulado(idPartido, idEvento, id(evento.annulsEventId()), minuto, minutoExtra);
            case "FOUL" -> servicio.procesarFalta(idPartido, idEvento, mapearEquipo(evento.team()), minuto, minutoExtra);
            case "FOUL_ANNULLED" -> servicio.procesarFaltaAnulada(idPartido, idEvento, id(evento.annulsEventId()), minuto, minutoExtra);
            case "YELLOW_CARD", "RED_CARD" ->
                    servicio.procesarTarjeta(idPartido, idEvento, evento.eventType(), mapearEquipo(evento.team()), minuto, minutoExtra);
            case "VAR_CHECK" -> servicio.procesarRevisionVar(idPartido, idEvento, id(evento.reviewsEventId()), minuto, minutoExtra);
            case "MATCH_CONTINUES" -> servicio.procesarJugadaConfirmada(idPartido, idEvento, id(evento.reviewsEventId()), minuto, minutoExtra);
            case "ADDED_TIME" -> servicio.procesarTiempoAnadido(idPartido, idEvento, minuto,
                    evento.minutosAgregados() == null ? 0 : evento.minutosAgregados());
            // CLOCK_TICK (el reloj avanzo), GOAL_CONFIRMED (un gol quedo firme) y cualquier otro:
            // solo se mueve el minuto, sin alerta.
            default -> servicio.actualizarMinuto(idPartido, minuto, minutoExtra);
        }
    }

    private IdEvento id(String valor) {
        return valor == null ? null : new IdEvento(valor);
    }

    private Equipo mapearEquipo(String team) {
        if (team == null) {
            return null;
        }
        return "home".equals(team) ? Equipo.LOCAL : Equipo.VISITANTE;
    }
}
