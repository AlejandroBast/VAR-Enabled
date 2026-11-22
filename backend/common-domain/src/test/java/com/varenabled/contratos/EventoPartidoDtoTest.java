package com.varenabled.contratos;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoPartidoDtoTest {

    @Test
    void noPermiteUnMatchIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("", "FOOTBALL", "GOAL", "home", "evt_g1", null, 10, null, null, Instant.now()));
    }

    @Test
    void noPermiteUnEventTypeNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("123", "FOOTBALL", null, "home", "evt_g1", null, 10, null, null, Instant.now()));
    }

    @Test
    void permiteUnMinutoNuloParaNoRomperMensajesDeKafkaAnterioresAEsteContrato() {
        assertDoesNotThrow(
                () -> new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, null, null, null, Instant.now()));
    }

    @Test
    void noPermiteUnMinutoFueraDeLos90Reglamentarios() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 91, null, null, Instant.now()));
    }

    @Test
    void noPermiteUnMinutoExtraMayorADiez() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 45, 11, null, Instant.now()));
    }
}
