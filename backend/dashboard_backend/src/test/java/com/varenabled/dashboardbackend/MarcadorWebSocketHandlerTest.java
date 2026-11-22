package com.varenabled.dashboardbackend;

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

    private MarcadorWebSocketHandler handler;

    @BeforeEach
    void preparar() {
        handler = new MarcadorWebSocketHandler();
        lenient().when(sesionA.isOpen()).thenReturn(true);
        lenient().when(sesionB.isOpen()).thenReturn(true);
    }

    @Test
    void transmiteElMensajeATodasLasSesionesConectadas() throws IOException {
        handler.afterConnectionEstablished(sesionA);
        handler.afterConnectionEstablished(sesionB);

        handler.transmitir("{\"matchId\":\"123\"}");

        verify(sesionA).sendMessage(new TextMessage("{\"matchId\":\"123\"}"));
        verify(sesionB).sendMessage(new TextMessage("{\"matchId\":\"123\"}"));
    }

    @Test
    void noLeEnviaNadaAUnaSesionYaCerrada() throws IOException {
        handler.afterConnectionEstablished(sesionA);
        handler.afterConnectionClosed(sesionA, null);

        handler.transmitir("{\"matchId\":\"123\"}");

        verify(sesionA, never()).sendMessage(any());
    }
}
