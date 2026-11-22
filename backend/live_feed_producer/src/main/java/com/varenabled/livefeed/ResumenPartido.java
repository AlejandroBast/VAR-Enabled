package com.varenabled.livefeed;

import java.util.List;

/**
 * Lo que devuelve el listado de partidos.
 * estado: POR_JUGAR, EN_JUEGO, EN_REVISION, ENTRETIEMPO o FINALIZADO.
 * revisionAbierta: el gol o la falta que el VAR esta revisando (si hay).
 * revisables: goles y faltas que todavia estan dentro de sus 2 minutos para revisar.
 */
public record ResumenPartido(String matchId, String homeTeam, String awayTeam, String estado,
                             Integer minuto, Integer minutoExtra, String revisionAbierta, List<String> revisables) {
}
