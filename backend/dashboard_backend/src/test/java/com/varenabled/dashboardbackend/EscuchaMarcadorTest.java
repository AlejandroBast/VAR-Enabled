package com.varenabled.dashboardbackend;

import com.varenabled.contratos.ActualizacionMarcadorDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscuchaMarcadorTest {

    @Mock
    private MarcadorWebSocketHandler handler;

    @Test
    void transmiteElMarcadorYLoGuardaParaLosQueSeConectenDespues() {
        EstadoDashboard estado = new EstadoDashboard();
        EscuchaMarcador listener = new EscuchaMarcador(handler, estado);
        ActualizacionMarcadorDto actualizacion = new ActualizacionMarcadorDto("123", 1, 0, 23, 0, "EN_JUEGO");

        listener.escuchar(actualizacion);

        MensajeDashboard esperado = new MensajeDashboard(MensajeDashboard.MARCADOR, actualizacion);
        verify(handler).transmitir(esperado);
        assertEquals(List.of(esperado), estado.mensajesActuales());
    }
}
