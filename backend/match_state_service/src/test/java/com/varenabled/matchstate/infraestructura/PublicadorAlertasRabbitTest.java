package com.varenabled.matchstate.infraestructura;

import com.varenabled.dominio.IdPartido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicadorAlertasRabbitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PublicadorAlertasRabbit adaptador;

    @Test
    void publicaLaAlertaEnElFanoutDeAlertas() {
        adaptador.publicarAlerta(new IdPartido("123"), "¡GOL!");

        verify(rabbitTemplate).convertAndSend(
                ConfiguracionRabbitMQ.EXCHANGE_ALERTAS,
                "",
                "¡GOL!");
    }
}
