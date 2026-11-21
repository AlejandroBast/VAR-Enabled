package com.varenabled.dominio.baloncesto;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidoBaloncestoTest {

    @Test
    void empiezaConElMarcadorEnCero() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));

        assertEquals(Marcador.enCero(), partido.obtenerMarcador());
    }

    @Test
    void anotarUnaCanastaDeTresSumaTresPuntos() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));

        partido.anotarCanasta(new IdEvento("evt_b1"), Equipo.LOCAL, 3);

        assertEquals(new Marcador(3, 0), partido.obtenerMarcador());
    }

    @Test
    void noPermiteCanastasConPuntosInvalidos() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));

        assertThrows(IllegalArgumentException.class,
                () -> partido.anotarCanasta(new IdEvento("evt_b1"), Equipo.LOCAL, 4));
    }

    @Test
    void anularCanastaPorReplayRevierteElMarcador() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));
        partido.anotarCanasta(new IdEvento("evt_b77"), Equipo.LOCAL, 3);

        partido.anularCanastaPorReplay(new IdEvento("evt_b77"));

        assertEquals(Marcador.enCero(), partido.obtenerMarcador());
    }

    @Test
    void anularUnaCanastaQueNoExisteLanzaExcepcion() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));

        assertThrows(EventoInexistenteException.class,
                () -> partido.anularCanastaPorReplay(new IdEvento("evt_fantasma")));
    }

    @Test
    void corregirTripleADobleDejaElMarcadorEnDos() {
        PartidoBaloncesto partido = new PartidoBaloncesto(new IdPartido("123"));
        partido.anotarCanasta(new IdEvento("evt_b77"), Equipo.LOCAL, 3);

        partido.anularCanastaPorReplay(new IdEvento("evt_b77"));
        partido.anotarCanasta(new IdEvento("evt_b78"), Equipo.LOCAL, 2);

        assertEquals(new Marcador(2, 0), partido.obtenerMarcador());
    }
}
