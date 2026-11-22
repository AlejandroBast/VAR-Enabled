package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.AlertaPartido;
import com.varenabled.contratos.AlertaPartidoDto;
import com.varenabled.dominio.Equipo;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        adaptador.publicarAlerta(new AlertaPartido(new IdPartido("123"), new IdEvento("evt_g1"), "GOAL",
                Equipo.LOCAL, null, 45, 2, "¡GOL!"));

        AlertaPartidoDto alerta = alertaPublicada();
        assertEquals("123", alerta.matchId());
        assertEquals("evt_g1", alerta.eventId());
        assertEquals("GOAL", alerta.eventType());
        assertEquals("home", alerta.team());
        assertNull(alerta.annulsEventId());
        assertEquals(45, alerta.minuto());
        assertEquals(2, alerta.minutoExtra());
        assertEquals("¡GOL!", alerta.mensaje());
        assertNotNull(alerta.timestamp());
    }

    @Test
    void unaAnulacionLlevaElIdDelGolAnulado() {
        adaptador.publicarAlerta(new AlertaPartido(new IdPartido("123"), new IdEvento("evt_a1"), "GOAL_ANNULLED",
                null, new IdEvento("evt_g1"), 12, 0, "¡GOL ANULADO POR EL VAR!"));

        AlertaPartidoDto alerta = alertaPublicada();
        assertEquals("evt_a1", alerta.eventId());
        assertEquals("evt_g1", alerta.annulsEventId());
        assertNull(alerta.team());
    }

    private AlertaPartidoDto alertaPublicada() {
        ArgumentCaptor<AlertaPartidoDto> captor = ArgumentCaptor.forClass(AlertaPartidoDto.class);
        verify(rabbitTemplate).convertAndSend(eq(ConfiguracionRabbitMQ.EXCHANGE_ALERTAS), eq(""), captor.capture());
        return captor.getValue();
    }
}
