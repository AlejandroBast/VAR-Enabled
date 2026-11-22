package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoPartidoDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("", "FOOTBALL", "GOAL", "home", "evt_g1", null, null));
    }

    @Test
    void noPermiteUnEventTypeNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("123", "FOOTBALL", null, "home", "evt_g1", null, null));
    }
}
