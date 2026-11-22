package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertaPartidoDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlertaPartidoDto("", "evt_g1", "GOAL", "home", null, 10, 0, "¡GOL!", Instant.now()));
    }

    @Test
    void noPermiteUnMensajeVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlertaPartidoDto("123", "evt_g1", "GOAL", "home", null, 10, 0, "", Instant.now()));
    }
}
