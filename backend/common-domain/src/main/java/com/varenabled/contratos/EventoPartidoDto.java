package com.varenabled.contratos;

import java.time.Instant;

public record EventoPartidoDto(
        String matchId,
        String sport,
        String eventType,
        String team,
        String eventId,
        String annulsEventId,
        Integer minuto,
        Integer minutoExtra,
        Integer minutosAgregados,
        Instant timestamp
) {

    public EventoPartidoDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("El eventType no puede ser nulo ni vacio");
        }
        if (minuto == null || minuto < 1 || minuto > 90) {
            throw new IllegalArgumentException("El minuto debe estar entre 1 y 90");
        }
        if (minutoExtra != null && (minutoExtra < 0 || minutoExtra > 10)) {
            throw new IllegalArgumentException("El minutoExtra debe estar entre 0 y 10");
        }
    }
}
