package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ComandoApuestaDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoApuestaDto("", "SUSPEND_BETS"));
    }

    @Test
    void noPermiteUnComandoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoApuestaDto("123", ""));
    }
}
