package com.varenabled.matchstate.infraestructura;

import com.varenabled.contratos.ActualizacionMarcadorDto;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.futbol.PartidoFutbol;
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
    void publicaElPartidoConEquiposYFaltasEnElExchangeConLaRoutingKeyDelPartido() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.nombrarEquipos("Nacional", "América");
        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);
        partido.registrarFalta(new IdEvento("evt_f1"), Equipo.VISITANTE);
        partido.actualizarMinuto(23, 0);
        partido.iniciarRevisionVar();
        partido.cambiarApuestas(com.varenabled.dominio.apuestas.EstadoApuestasPartido.DETENIDAS);

        adaptador.publicarMarcador(partido);

        verify(rabbitTemplate).convertAndSend(
                ConfiguracionRabbitMQ.EXCHANGE_MARCADOR,
                "score.match_123",
                new ActualizacionMarcadorDto("123", 1, 0, 23, 0, "EN_REVISION", "Nacional", "América", 0, 1, "DETENIDAS"));
    }
}
