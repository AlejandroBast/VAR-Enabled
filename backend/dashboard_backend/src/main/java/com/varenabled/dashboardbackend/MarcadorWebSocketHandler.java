package com.varenabled.dashboardbackend;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MarcadorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sesiones = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        sesiones.add(sesion);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, org.springframework.web.socket.CloseStatus status) {
        sesiones.remove(sesion);
    }

    public void transmitir(String mensajeJson) {
        TextMessage mensaje = new TextMessage(mensajeJson);
        for (WebSocketSession sesion : sesiones) {
            try {
                if (sesion.isOpen()) {
                    sesion.sendMessage(mensaje);
                }
            } catch (IOException e) {
                sesiones.remove(sesion);
            }
        }
    }
}
