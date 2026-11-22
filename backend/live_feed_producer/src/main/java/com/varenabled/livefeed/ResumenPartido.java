package com.varenabled.livefeed;

/** Lo que devuelve el listado de partidos: estado POR_JUGAR, EN_JUEGO o FINALIZADO y el minuto actual. */
public record ResumenPartido(String matchId, String estado, Integer minuto, Integer minutoExtra) {
}
