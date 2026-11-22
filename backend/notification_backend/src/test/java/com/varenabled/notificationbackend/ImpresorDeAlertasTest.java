package com.varenabled.notificationbackend;

import com.varenabled.contratos.AlertaPartidoDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpresorDeAlertasTest {

    private final ImpresorDeAlertas impresor = new ImpresorDeAlertas();

    @Test
    void elMensajeFormateadoIncluyeMatchIdEventIdTipoMinutoHoraYMensaje() {
        Instant hora = Instant.parse("2026-11-22T20:30:10Z");
        AlertaPartidoDto alerta = new AlertaPartidoDto("123", "evt_a_3f9a1c2e", "GOAL_ANNULLED", null, "evt_g_1",
                45, 2, "¡GOL ANULADO POR EL VAR!", hora);

        String formateado = impresor.formatear(alerta);

        assertTrue(formateado.contains("123"));
        assertTrue(formateado.contains("evt_a_3f9a1c2e"));
        assertTrue(formateado.contains("GOAL_ANNULLED"));
        assertTrue(formateado.contains("45+2'"));
        assertTrue(formateado.contains(hora.toString()));
        assertTrue(formateado.contains("¡GOL ANULADO POR EL VAR!"));
    }
}
