package com.varenabled.dominio.apuestas;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglasSuspensionApuestasTest {

    @Test
    void enFutbolElGolSuspende() {
        assertEquals(Optional.of(ComandoApuesta.SUSPENDER), ReglasSuspensionApuestas.evaluar("GOAL"));
    }

    @Test
    void enFutbolElVarCheckSuspende() {
        assertEquals(Optional.of(ComandoApuesta.SUSPENDER), ReglasSuspensionApuestas.evaluar("VAR_CHECK"));
    }

    @Test
    void enFutbolElGolAnuladoReanuda() {
        assertEquals(Optional.of(ComandoApuesta.REANUDAR), ReglasSuspensionApuestas.evaluar("GOAL_ANNULLED"));
    }

    @Test
    void elJuegoQueSigueReanuda() {
        assertEquals(Optional.of(ComandoApuesta.REANUDAR), ReglasSuspensionApuestas.evaluar("MATCH_CONTINUES"));
    }

    @Test
    void unaFaltaNoSuspende() {
        assertEquals(Optional.empty(), ReglasSuspensionApuestas.evaluar("FOUL"));
    }

    @Test
    void unEventoQueNoAfectaLasApuestasNoHaceNada() {
        assertEquals(Optional.empty(), ReglasSuspensionApuestas.evaluar("MATCH_STARTED"));
    }
}
