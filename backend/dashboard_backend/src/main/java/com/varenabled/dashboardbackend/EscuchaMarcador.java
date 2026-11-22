package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varenabled.contratos.ActualizacionMarcadorDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaMarcador {

    private final MarcadorWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public EscuchaMarcador(MarcadorWebSocketHandler handler, ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_MARCADOR)
    public void escuchar(ActualizacionMarcadorDto actualizacion) throws JsonProcessingException {
        handler.transmitir(objectMapper.writeValueAsString(actualizacion));
    }
}
