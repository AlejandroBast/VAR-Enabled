package com.varenabled.contratos;

public record ActualizacionMarcadorDto(
        String matchId,
        int local,
        int visitante,
        int minuto,
        int minutoExtra,
        String estado,
        String equipoLocal,
        String equipoVisitante,
        int faltasLocal,
        int faltasVisitante
) {

    public ActualizacionMarcadorDto {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("El matchId no puede ser nulo ni vacio");
        }
        if (local < 0 || visitante < 0) {
            throw new IllegalArgumentException("El marcador no puede ser negativo");
        }
        if (faltasLocal < 0 || faltasVisitante < 0) {
            throw new IllegalArgumentException("Las faltas no pueden ser negativas");
        }
    }

    /** Forma corta, sin nombres de equipos ni faltas. */
    public ActualizacionMarcadorDto(String matchId, int local, int visitante, int minuto, int minutoExtra, String estado) {
        this(matchId, local, visitante, minuto, minutoExtra, estado, null, null, 0, 0);
    }
}
