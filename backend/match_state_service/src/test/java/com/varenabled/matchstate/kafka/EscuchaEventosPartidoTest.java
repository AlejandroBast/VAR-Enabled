package com.varenabled.matchstate.kafka;

import com.varenabled.aplicacion.futbol.RepositorioPartidoFutbolPort;
import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.futbol.PartidoFutbol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "match_events")
@DirtiesContext
class EscuchaEventosPartidoTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RepositorioPartidoFutbolPort repositorio;

    @Test
    void unGolAnuladoDejaElMarcadorEnCero() throws InterruptedException {
        // Arrange
        IdPartido idPartido = new IdPartido("123");
        EventoPartidoDto gol = new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g123", null, null);
        EventoPartidoDto golAnulado = new EventoPartidoDto("123", "FOOTBALL", "GOAL_ANNULLED", null, null, "evt_g123", null);

        // Act
        kafkaTemplate.send("match_events", "123", gol);
        kafkaTemplate.send("match_events", "123", golAnulado);

        // Assert
        Marcador marcadorFinal = esperarMarcador(idPartido, Marcador.enCero());
        assertEquals(Marcador.enCero(), marcadorFinal);
    }

    private Marcador esperarMarcador(IdPartido idPartido, Marcador esperado) throws InterruptedException {
        for (int intento = 0; intento < 50; intento++) {
            Optional<PartidoFutbol> partido = repositorio.buscarPorId(idPartido);
            if (partido.isPresent() && partido.get().obtenerMarcador().equals(esperado)) {
                return partido.get().obtenerMarcador();
            }
            Thread.sleep(100);
        }
        return repositorio.buscarPorId(idPartido).map(PartidoFutbol::obtenerMarcador).orElse(null);
    }
}
