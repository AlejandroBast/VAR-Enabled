package com.varenabled.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IdEventoTest {

    @Test
    void noPermiteUnValorNulo() {
        assertThrows(IllegalArgumentException.class, () -> new IdEvento(null));
    }

    @Test
    void noPermiteUnValorVacio() {
        assertThrows(IllegalArgumentException.class, () -> new IdEvento("   "));
    }
}
