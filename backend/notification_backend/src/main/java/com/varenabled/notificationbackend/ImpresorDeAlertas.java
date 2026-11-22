package com.varenabled.notificationbackend;

import com.varenabled.contratos.AlertaPartidoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ImpresorDeAlertas {

    private static final Logger log = LoggerFactory.getLogger(ImpresorDeAlertas.class);

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_ALERTAS)
    public void escuchar(AlertaPartidoDto alerta) {
        log.info(formatear(alerta));
    }

    String formatear(AlertaPartidoDto alerta) {
        return "[ALERTA] partido=%s evento=%s hora=%s -> %s".formatted(
                alerta.matchId(), alerta.eventId(), alerta.timestamp(), alerta.mensaje());
    }
}
