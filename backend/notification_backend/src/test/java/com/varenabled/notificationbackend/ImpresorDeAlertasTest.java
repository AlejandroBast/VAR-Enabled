package com.varenabled.notificationbackend;

import com.varenabled.contratos.AlertaPartidoDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpresorDeAlertasTest {

    private final ImpresorDeAlertas impresor = new ImpresorDeAlertas();

    @Test
    void elMensajeFormateadoIncluyeMatchIdEventIdHoraYMensaje() {
        Instant hora = Instant.parse("2026-11-22T20:30:10Z");
        AlertaPartidoDto alerta = new AlertaPartidoDto("123", "evt_a_3f9a1c2e", "¡GOL ANULADO POR EL VAR!", hora);

        String formateado = impresor.formatear(alerta);

        assertTrue(formateado.contains("123"));
        assertTrue(formateado.contains("evt_a_3f9a1c2e"));
        assertTrue(formateado.contains(hora.toString()));
        assertTrue(formateado.contains("¡GOL ANULADO POR EL VAR!"));
    }
}
