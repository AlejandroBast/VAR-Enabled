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
    void enCualquierDeporteSigueElJuegoReanuda() {
        assertEquals(Optional.of(ComandoApuesta.REANUDAR), ReglasSuspensionApuestas.evaluar("MATCH_CONTINUES"));
    }

    @Test
    void enBaloncestoUnaCanastaNoSuspende() {
        assertEquals(Optional.empty(), ReglasSuspensionApuestas.evaluar("BASKET"));
    }

    @Test
    void enBaloncestoElReviewCheckSuspende() {
        assertEquals(Optional.of(ComandoApuesta.SUSPENDER), ReglasSuspensionApuestas.evaluar("REVIEW_CHECK"));
    }

    @Test
    void enBaloncestoLaCanastaAnuladaReanuda() {
        assertEquals(Optional.of(ComandoApuesta.REANUDAR), ReglasSuspensionApuestas.evaluar("BASKET_ANNULLED"));
    }

    @Test
    void enVoleibolElChallengeSuspende() {
        assertEquals(Optional.of(ComandoApuesta.SUSPENDER), ReglasSuspensionApuestas.evaluar("CHALLENGE_REQUESTED"));
    }

    @Test
    void enVoleibolGanarElSetSuspende() {
        assertEquals(Optional.of(ComandoApuesta.SUSPENDER), ReglasSuspensionApuestas.evaluar("SET_WON"));
    }

    @Test
    void enVoleibolElPuntoAnuladoReanuda() {
        assertEquals(Optional.of(ComandoApuesta.REANUDAR), ReglasSuspensionApuestas.evaluar("POINT_ANNULLED"));
    }

    @Test
    void unEventoQueNoAfectaLasApuestasNoHaceNada() {
        assertEquals(Optional.empty(), ReglasSuspensionApuestas.evaluar("MATCH_STARTED"));
    }
}
