package com.varenabled.contratos;

import java.time.Instant;

public record AlertaPartidoDto(
        String matchId,
        String eventId,
        String eventType,
        String team,
        String annulsEventId,
        int minuto,
        int minutoExtra,
        String mensaje,
        Instant timestamp,
        String reviewsEventId
) {

    public AlertaPartidoDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede ser nulo ni vacio");
        }
    }

    /** Forma corta, para las alertas que no se refieren a una revision. */
    public AlertaPartidoDto(String matchId, String eventId, String eventType, String team, String annulsEventId,
                            int minuto, int minutoExtra, String mensaje, Instant timestamp) {
        this(matchId, eventId, eventType, team, annulsEventId, minuto, minutoExtra, mensaje, timestamp, null);
    }
}
