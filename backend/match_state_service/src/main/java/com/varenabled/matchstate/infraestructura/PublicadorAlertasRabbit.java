package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.dominio.IdPartido;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorAlertasRabbit implements PublicadorAlertasPort {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorAlertasRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarAlerta(IdPartido idPartido, String mensaje) {
        rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS, "", mensaje);
    }
}
