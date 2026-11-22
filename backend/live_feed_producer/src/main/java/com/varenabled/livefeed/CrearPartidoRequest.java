package com.varenabled.livefeed;

/** home_team y away_team son opcionales: si no vienen, el dashboard muestra "Local" y "Visitante". */
public record CrearPartidoRequest(String matchId, String homeTeam, String awayTeam) {
}
