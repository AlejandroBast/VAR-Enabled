package com.varenabled.contratos;

import java.time.Instant;

public record EventoPartidoDto(
        String matchId,
        String sport,
        String eventType,
        String team,
        String eventId,
        String annulsEventId,
        Integer points,
        Instant timestamp
) {

    public EventoPartidoDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("El eventType no puede ser nulo ni vacio");
        }
    }
}
