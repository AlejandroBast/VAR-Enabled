package com.varenabled.dashboardbackend;

import com.varenabled.contratos.AlertaPartidoDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscuchaEventosTest {

    @Mock
    private MarcadorWebSocketHandler handler;

    private final AlertaPartidoDto revision = new AlertaPartidoDto(
            "123", "evt_v1", "VAR_CHECK", null, null, 30, 0, "Revisión VAR en curso", Instant.now());

    @Test
    void transmiteElEventoAlFrontend() {
        EscuchaEventos listener = new EscuchaEventos(handler, new EstadoDashboard());

        listener.escuchar(revision);

        verify(handler).transmitir(new MensajeDashboard(MensajeDashboard.EVENTO, revision));
    }

    @Test
    void noRepiteUnEventoQueYaLlegoAntes() {
        EscuchaEventos listener = new EscuchaEventos(handler, new EstadoDashboard());

        listener.escuchar(revision);
        listener.escuchar(revision);

        verify(handler, times(1)).transmitir(new MensajeDashboard(MensajeDashboard.EVENTO, revision));
    }
}
