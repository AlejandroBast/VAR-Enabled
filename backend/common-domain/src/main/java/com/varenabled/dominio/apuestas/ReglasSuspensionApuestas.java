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
