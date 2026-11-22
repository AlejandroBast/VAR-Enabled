package com.varenabled.historicalarchiver.aplicacion;

import java.time.Duration;

public record RevisionVarRegistrada(
        String matchId,
        String deporte,
        String tipoRevision,
        String eventoAnulado,
        Duration duracion
) {
}
