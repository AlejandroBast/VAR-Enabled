package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MarcadorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sesiones = new CopyOnWriteArraySet<>();
    private final EstadoDashboard estado;
    private final ObjectMapper objectMapper;

    public MarcadorWebSocketHandler(EstadoDashboard estado, ObjectMapper objectMapper) {
        this.estado = estado;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        sesiones.add(sesion);
        for (MensajeDashboard mensaje : estado.mensajesActuales()) {
            enviar(sesion, convertir(mensaje));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus status) {
        sesiones.remove(sesion);
    }

    public void transmitir(MensajeDashboard mensaje) {
        TextMessage texto = convertir(mensaje);
        for (WebSocketSession sesion : sesiones) {
            enviar(sesion, texto);
        }
    }

    private TextMessage convertir(MensajeDashboard mensaje) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(mensaje));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir el mensaje a JSON", e);
        }
    }

    private void enviar(WebSocketSession sesion, TextMessage texto) {
        try {
            // Varios hilos pueden escribir a la vez (marcador y eventos), una sesion no lo soporta.
            synchronized (sesion) {
                if (sesion.isOpen()) {
                    sesion.sendMessage(texto);
                }
            }
        } catch (IOException e) {
            sesiones.remove(sesion);
        }
    }
}
