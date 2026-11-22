package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.contratos.AlertaPartidoDto;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PublicadorAlertasRabbit implements PublicadorAlertasPort {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorAlertasRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarAlerta(IdPartido idPartido, IdEvento idEvento, String mensaje) {
        AlertaPartidoDto alerta = new AlertaPartidoDto(idPartido.valor(), idEvento.valor(), mensaje, Instant.now());
        rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS, "", alerta);
    }
}
