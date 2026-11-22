package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varenabled.contratos.ActualizacionMarcadorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarcadorWebSocketHandlerTest {

    @Mock
    private WebSocketSession sesionA;

    @Mock
    private WebSocketSession sesionB;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ActualizacionMarcadorDto marcador = new ActualizacionMarcadorDto("123", 1, 0, 23, 0, "EN_JUEGO");
    private EstadoDashboard estado;
    private MarcadorWebSocketHandler handler;

    @BeforeEach
    void preparar() {
        estado = new EstadoDashboard();
        handler = new MarcadorWebSocketHandler(estado, mapper);
        lenient().when(sesionA.isOpen()).thenReturn(true);
        lenient().when(sesionB.isOpen()).thenReturn(true);
    }

    @Test
    void transmiteElMensajeATodasLasSesionesConectadas() throws IOException {
        handler.afterConnectionEstablished(sesionA);
        handler.afterConnectionEstablished(sesionB);

        MensajeDashboard mensaje = new MensajeDashboard(MensajeDashboard.MARCADOR, marcador);
        handler.transmitir(mensaje);

        TextMessage esperado = new TextMessage(mapper.writeValueAsString(mensaje));
        verify(sesionA).sendMessage(esperado);
        verify(sesionB).sendMessage(esperado);
    }

    @Test
    void alConectarseLeEnviaLoQueYaPasoEnLosPartidos() throws IOException {
        estado.registrarMarcador(marcador);

        handler.afterConnectionEstablished(sesionA);

        MensajeDashboard mensaje = new MensajeDashboard(MensajeDashboard.MARCADOR, marcador);
        verify(sesionA).sendMessage(new TextMessage(mapper.writeValueAsString(mensaje)));
    }

    @Test
    void noLeEnviaNadaAUnaSesionYaCerrada() throws IOException {
        handler.afterConnectionEstablished(sesionA);
        handler.afterConnectionClosed(sesionA, null);

        handler.transmitir(new MensajeDashboard(MensajeDashboard.MARCADOR, marcador));

        verify(sesionA, never()).sendMessage(any());
    }
}
