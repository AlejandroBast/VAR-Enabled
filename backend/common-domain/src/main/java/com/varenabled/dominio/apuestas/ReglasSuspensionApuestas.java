package com.varenabled.dominio.apuestas;

import java.util.Optional;
import java.util.Set;

public final class ReglasSuspensionApuestas {

    // Un gol, una revision VAR o el final del partido cierran las apuestas.
    private static final Set<String> EVENTOS_QUE_SUSPENDEN = Set.of("GOAL", "VAR_CHECK", "MATCH_ENDED");

    // Se reabren cuando la jugada queda resuelta: el VAR la anula o la confirma, o el gol queda firme
    // porque pasaron sus 2 minutos sin revision.
    private static final Set<String> EVENTOS_QUE_REANUDAN = Set.of(
            "GOAL_ANNULLED", "FOUL_ANNULLED", "MATCH_CONTINUES", "GOAL_CONFIRMED");

    private ReglasSuspensionApuestas() {
    }

    /**
     * Como quedan las apuestas para el publico despues de un evento (vacio si el evento no las cambia).
     * Es la misma regla que evaluar(), pero distingue por que estan suspendidas:
     * CERRADAS por un gol o el final, DETENIDAS por una revision VAR.
     */
    public static Optional<EstadoApuestasPartido> estadoDespuesDe(String eventType) {
        if ("VAR_CHECK".equals(eventType)) {
            return Optional.of(EstadoApuestasPartido.DETENIDAS);
        }
        if (EVENTOS_QUE_SUSPENDEN.contains(eventType)) {
            return Optional.of(EstadoApuestasPartido.CERRADAS);
        }
        if (EVENTOS_QUE_REANUDAN.contains(eventType) || "MATCH_STARTED".equals(eventType)) {
            return Optional.of(EstadoApuestasPartido.ABIERTAS);
        }
        return Optional.empty();
    }

    public static Optional<ComandoApuesta> evaluar(String eventType) {
        if (EVENTOS_QUE_SUSPENDEN.contains(eventType)) {
            return Optional.of(ComandoApuesta.SUSPENDER);
        }
        if (EVENTOS_QUE_REANUDAN.contains(eventType)) {
            return Optional.of(ComandoApuesta.REANUDAR);
        }
        return Optional.empty();
    }
}
