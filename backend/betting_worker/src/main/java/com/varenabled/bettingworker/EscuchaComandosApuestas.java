package com.varenabled.bettingworker;

import com.varenabled.aplicacion.apuestas.EstadoApuestas;
import com.varenabled.aplicacion.apuestas.RegistroEstadoApuestas;
import com.varenabled.contratos.ComandoApuestaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaComandosApuestas {

    private static final Logger log = LoggerFactory.getLogger(EscuchaComandosApuestas.class);

    private final RegistroEstadoApuestas registro;

    public EscuchaComandosApuestas(RegistroEstadoApuestas registro) {
        this.registro = registro;
    }

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_COMANDOS)
    public void escuchar(ComandoApuestaDto comando) {
        EstadoApuestas nuevoEstado = "SUSPEND_BETS".equals(comando.comando())
                ? EstadoApuestas.SUSPENDIDO
                : EstadoApuestas.ABIERTO;

        boolean cambio = registro.aplicar(comando.matchId(), nuevoEstado);

        if (cambio) {
            log.info("Partido {}: apuestas {}", comando.matchId(), nuevoEstado);
        } else {
            log.info("Partido {}: comando repetido, ya estaba {}", comando.matchId(), nuevoEstado);
        }
    }
}
