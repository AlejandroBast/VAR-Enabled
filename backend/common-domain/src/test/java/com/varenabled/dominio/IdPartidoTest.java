package com.varenabled.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IdPartidoTest {

    @Test
    void noPermiteUnValorNulo() {
        assertThrows(IllegalArgumentException.class, () -> new IdPartido(null));
    }

    @Test
    void noPermiteUnValorVacio() {
        assertThrows(IllegalArgumentException.class, () -> new IdPartido(""));
    }
}
