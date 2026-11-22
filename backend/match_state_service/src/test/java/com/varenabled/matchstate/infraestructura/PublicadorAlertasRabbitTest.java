package com.varenabled.matchstate.infraestructura;

import com.varenabled.contratos.AlertaPartidoDto;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicadorAlertasRabbitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PublicadorAlertasRabbit adaptador;

    @Test
    void publicaLaAlertaEnElFanoutDeAlertasConTrazabilidad() {
        adaptador.publicarAlerta(new IdPartido("123"), new IdEvento("evt_g1"), "¡GOL!");

        ArgumentCaptor<AlertaPartidoDto> captor = ArgumentCaptor.forClass(AlertaPartidoDto.class);
        verify(rabbitTemplate).convertAndSend(eq(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS), eq(""), captor.capture());

        AlertaPartidoDto alerta = captor.getValue();
        assertEquals("123", alerta.matchId());
        assertEquals("evt_g1", alerta.eventId());
        assertEquals("¡GOL!", alerta.mensaje());
        assertNotNull(alerta.timestamp());
    }
}
