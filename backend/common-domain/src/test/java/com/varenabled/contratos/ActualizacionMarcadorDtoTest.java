package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ActualizacionMarcadorDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActualizacionMarcadorDto("", 1, 0, 10, 0, "EN_JUEGO"));
    }

    @Test
    void noPermiteValoresNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActualizacionMarcadorDto("123", -1, 0, 10, 0, "EN_JUEGO"));
    }
}
