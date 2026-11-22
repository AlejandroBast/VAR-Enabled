package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoPartidoDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("", "FOOTBALL", "GOAL", "home", "evt_g1", null, null, Instant.now()));
    }

    @Test
    void noPermiteUnEventTypeNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("123", "FOOTBALL", null, "home", "evt_g1", null, null, Instant.now()));
    }
}
