package com.varenabled.dominio.apuestas;

import java.util.Optional;
import java.util.Set;

public final class ReglasSuspensionApuestas {

    private static final Set<String> EVENTOS_QUE_SUSPENDEN = Set.of("GOAL", "VAR_CHECK");

    private static final Set<String> EVENTOS_QUE_REANUDAN = Set.of("GOAL_ANNULLED", "MATCH_CONTINUES");

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
