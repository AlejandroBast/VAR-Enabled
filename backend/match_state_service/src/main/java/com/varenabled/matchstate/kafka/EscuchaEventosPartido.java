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
        IdEvento idEvento = evento.eventId() == null ? null : new IdEvento(evento.eventId());
        int minuto = evento.minuto() == null ? 0 : evento.minuto();
        int minutoExtra = evento.minutoExtra() == null ? 0 : evento.minutoExtra();

        switch (evento.eventType()) {
            case "GOAL" -> servicio.procesarGol(idPartido, idEvento, mapearEquipo(evento.team()), minuto, minutoExtra);
            case "GOAL_ANNULLED" -> servicio.procesarGolAnulado(idPartido, idEvento, new IdEvento(evento.annulsEventId()), minuto, minutoExtra);
            case "VAR_CHECK" -> servicio.procesarRevisionVar(idPartido, idEvento, minuto, minutoExtra);
            case "MATCH_CONTINUES" -> servicio.procesarJugadaConfirmada(idPartido, idEvento, minuto, minutoExtra);
            case "FOUL" -> servicio.procesarFalta(idPartido, idEvento, mapearEquipo(evento.team()), minuto, minutoExtra);
            case "ADDED_TIME" -> servicio.procesarTiempoAnadido(idPartido, idEvento, minuto,
                    evento.minutosAgregados() == null ? 0 : evento.minutosAgregados());
            default -> servicio.actualizarMinuto(idPartido, minuto, minutoExtra);
        }
    }

    private Equipo mapearEquipo(String team) {
        if (team == null) {
            return null;
        }
        return "home".equals(team) ? Equipo.LOCAL : Equipo.VISITANTE;
    }
}
