package com.varenabled.dashboardbackend;

import com.varenabled.contratos.ActualizacionMarcadorDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaMarcador {

    private final MarcadorWebSocketHandler handler;
    private final EstadoDashboard estado;

    public EscuchaMarcador(MarcadorWebSocketHandler handler, EstadoDashboard estado) {
        this.handler = handler;
        this.estado = estado;
    }

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_MARCADOR)
    public void escuchar(ActualizacionMarcadorDto actualizacion) {
        estado.registrarMarcador(actualizacion);
        handler.transmitir(new MensajeDashboard(MensajeDashboard.MARCADOR, actualizacion));
    }
}
