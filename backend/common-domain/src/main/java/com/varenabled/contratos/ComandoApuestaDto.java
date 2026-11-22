package com.varenabled.contratos;

public record ComandoApuestaDto(
        String matchId,
        String comando
) {

    public ComandoApuestaDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (comando == null || comando.isBlank()) {
            throw new IllegalArgumentException("El comando no puede ser nulo ni vacio");
        }
    }
}
