package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.contratos.ActualizacionMarcadorDto;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorMarcadorRabbit implements PublicadorMarcadorPort {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorMarcadorRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarMarcador(IdPartido idPartido, Marcador marcador) {
        String routingKey = "score.match_" + idPartido.valor();
        ActualizacionMarcadorDto actualizacion = new ActualizacionMarcadorDto(
                idPartido.valor(), marcador.local(), marcador.visitante());

        rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.EXCHANGE_MARCADOR, routingKey, actualizacion);
    }
}
