package com.varenabled.matchstate.infraestructura;

import com.varenabled.contratos.ActualizacionMarcadorDto;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicadorMarcadorRabbitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PublicadorMarcadorRabbit adaptador;

    @Test
    void publicaElMarcadorEnElExchangeConLaRoutingKeyDelPartido() {
        adaptador.publicarMarcador(new IdPartido("123"), new Marcador(1, 0), 23, 0);

        verify(rabbitTemplate).convertAndSend(
                ConfiguracionRabbitMQ.EXCHANGE_MARCADOR,
                "score.match_123",
                new ActualizacionMarcadorDto("123", 1, 0, 23, 0));
    }
}
