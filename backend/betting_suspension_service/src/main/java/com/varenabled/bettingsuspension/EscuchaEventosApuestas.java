package com.varenabled.bettingsuspension;

import com.varenabled.contratos.ComandoApuestaDto;
import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.apuestas.ReglasSuspensionApuestas;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaEventosApuestas {

    private final RabbitTemplate rabbitTemplate;

    public EscuchaEventosApuestas(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @KafkaListener(topics = "match_events", groupId = "betting-suspension-service")
    public void escuchar(EventoPartidoDto evento) {
        ReglasSuspensionApuestas.evaluar(evento.eventType()).ifPresent(comando -> {
            ComandoApuestaDto comandoApuesta = new ComandoApuestaDto(evento.matchId(), comando.codigo());
            rabbitTemplate.convertAndSend(ConfiguracionRabbitMQ.COLA_COMANDOS, comandoApuesta);
        });
    }
}
