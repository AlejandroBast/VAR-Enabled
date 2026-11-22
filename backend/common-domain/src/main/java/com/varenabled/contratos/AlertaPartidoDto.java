package com.varenabled.contratos;

import java.time.Instant;

public record AlertaPartidoDto(
        String matchId,
        String eventId,
        String mensaje,
        Instant timestamp
) {

    public AlertaPartidoDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede ser nulo ni vacio");
        }
    }
}
