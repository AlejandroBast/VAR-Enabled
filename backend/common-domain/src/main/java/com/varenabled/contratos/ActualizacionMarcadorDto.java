package com.varenabled.contratos;

public record ActualizacionMarcadorDto(
        String matchId,
        int local,
        int visitante,
        int minuto,
        int minutoExtra,
        String estado
) {

    public ActualizacionMarcadorDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (local < 0 || visitante < 0) {
            throw new IllegalArgumentException("El marcador no puede ser negativo");
        }
    }
}
