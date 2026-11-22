package com.varenabled.dashboardbackend;

import com.varenabled.contratos.AlertaPartidoDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaEventos {

    private final MarcadorWebSocketHandler handler;
    private final EstadoDashboard estado;

    public EscuchaEventos(MarcadorWebSocketHandler handler, EstadoDashboard estado) {
        this.handler = handler;
        this.estado = estado;
    }

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_ALERTAS)
    public void escuchar(AlertaPartidoDto evento) {
        if (estado.registrarEvento(evento)) {
            handler.transmitir(new MensajeDashboard(MensajeDashboard.EVENTO, evento));
        }
    }
}
