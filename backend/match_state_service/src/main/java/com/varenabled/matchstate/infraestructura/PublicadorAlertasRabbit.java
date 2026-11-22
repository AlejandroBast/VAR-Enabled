package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.AlertaPartido;
import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.contratos.AlertaPartidoDto;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
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
    public void publicarAlerta(AlertaPartido alerta) {
        AlertaPartidoDto dto = new AlertaPartidoDto(
                alerta.idPartido().valor(),
                valorDe(alerta.idEvento()),
                alerta.tipoEvento(),
                equipoComoTexto(alerta.equipo()),
                valorDe(alerta.idEventoAnulado()),
                alerta.minuto(),
                alerta.minutoExtra(),
                alerta.mensaje(),
                Instant.now());
        rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS, "", dto);
    }

    private String valorDe(IdEvento idEvento) {
        return idEvento == null ? null : idEvento.valor();
    }

    private String equipoComoTexto(Equipo equipo) {
        if (equipo == null) {
            return null;
        }
        return equipo == Equipo.LOCAL ? "home" : "away";
    }
}
