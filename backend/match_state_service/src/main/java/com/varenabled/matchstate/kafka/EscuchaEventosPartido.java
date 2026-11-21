package com.varenabled.matchstate.kafka;

import com.varenabled.aplicacion.futbol.ServicioMarcadorFutbol;
import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaEventosPartido {

    private final ServicioMarcadorFutbol servicio;

    public EscuchaEventosPartido(ServicioMarcadorFutbol servicio) {
        this.servicio = servicio;
    }

    @KafkaListener(topics = "match_events", groupId = "match-state-service")
    public void escuchar(EventoPartidoDto evento) {
        IdPartido idPartido = new IdPartido(evento.matchId());

        switch (evento.eventType()) {
            case "GOAL" -> servicio.procesarGol(idPartido, new IdEvento(evento.eventId()), mapearEquipo(evento.team()));
            case "GOAL_ANNULLED" -> servicio.procesarGolAnulado(idPartido, new IdEvento(evento.annulsEventId()));
            default -> {
                // los demas eventos y deportes se incorporan en fases futuras
            }
        }
    }

    private Equipo mapearEquipo(String team) {
        return "home".equals(team) ? Equipo.LOCAL : Equipo.VISITANTE;
    }
}
