package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.contratos.ActualizacionMarcadorDto;
import com.varenabled.dominio.futbol.PartidoFutbol;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorMarcadorRabbit implements PublicadorMarcadorPort {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorMarcadorRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarMarcador(PartidoFutbol partido) {
        String matchId = partido.obtenerId().valor();
        ActualizacionMarcadorDto actualizacion = new ActualizacionMarcadorDto(
                matchId,
                partido.obtenerMarcador().local(),
                partido.obtenerMarcador().visitante(),
                partido.obtenerMinutoActual(),
                partido.obtenerMinutoExtraActual(),
                partido.obtenerEstado().name(),
                partido.obtenerEquipoLocal(),
                partido.obtenerEquipoVisitante(),
                partido.obtenerFaltas().local(),
                partido.obtenerFaltas().visitante());

        rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.EXCHANGE_MARCADOR, "score.match_" + matchId, actualizacion);
    }
}
