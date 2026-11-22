package com.varenabled.historicalarchiver.persistencia;

import com.varenabled.contratos.EventoPartidoDto;

public final class MapeadorEventoArchivado {

    private MapeadorEventoArchivado() {
    }

    public static MatchEventLogEntity aEntidad(EventoPartidoDto evento) {
        return new MatchEventLogEntity(
                evento.matchId(),
                evento.sport(),
                evento.eventType(),
                evento.team(),
                evento.eventId(),
                evento.annulsEventId(),
                evento.points(),
                evento.timestamp());
    }

    public static EventoPartidoDto aDto(MatchEventLogEntity entidad) {
        return new EventoPartidoDto(
                entidad.getMatchId(),
                entidad.getSport(),
                entidad.getEventType(),
                entidad.getTeam(),
                entidad.getEventId(),
                entidad.getAnnulsEventId(),
                entidad.getPoints(),
                entidad.getTimestamp());
    }
}
