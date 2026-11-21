package com.varenabled.dominio.voleibol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidoVoleibolTest {

    @Test
    void empiezaConElMarcadorDelSetEnCero() {
        PartidoVoleibol partido = new PartidoVoleibol(new IdPartido("456"));

        assertEquals(Marcador.enCero(), partido.obtenerMarcadorDelSetActual());
    }

    @Test
    void anotarPuntoDelLocalAumentaElMarcadorDelSetActual() {
        PartidoVoleibol partido = new PartidoVoleibol(new IdPartido("456"));

        partido.anotarPunto(new IdEvento("evt_p1"), Equipo.LOCAL);

        assertEquals(new Marcador(1, 0), partido.obtenerMarcadorDelSetActual());
    }

    @Test
    void anularUnPuntoQueNoExisteLanzaExcepcion() {
        PartidoVoleibol partido = new PartidoVoleibol(new IdPartido("456"));

        assertThrows(EventoInexistenteException.class,
                () -> partido.anularPuntoPorChallenge(new IdEvento("evt_fantasma")));
    }

    @Test
    void ganarElSetConVeinticincoPuntosAbreUnSetNuevo() {
        PartidoVoleibol partido = new PartidoVoleibol(new IdPartido("456"));
        anotarVariosPuntos(partido, Equipo.LOCAL, 25);

        assertEquals(1, partido.obtenerSetsGanadosLocal());
        assertEquals(Marcador.enCero(), partido.obtenerMarcadorDelSetActual());
    }

    @Test
    void anularElPuntoQueGanoElSetLoReabre() {
        // Arrange: el set va 24-23 y llega el punto que lo cierra 25-23
        PartidoVoleibol partido = new PartidoVoleibol(new IdPartido("456"));
        anotarVariosPuntos(partido, Equipo.LOCAL, 24);
        anotarVariosPuntos(partido, Equipo.VISITANTE, 23);
        IdEvento idPuntoGanador = new IdEvento("evt_p40");
        partido.anotarPunto(idPuntoGanador, Equipo.LOCAL);
        assertEquals(1, partido.obtenerSetsGanadosLocal());

        // Act: el VAR anula ese punto
        partido.anularPuntoPorChallenge(idPuntoGanador);

        // Assert: el set se reabre en 24-23
        assertEquals(0, partido.obtenerSetsGanadosLocal());
        assertEquals(new Marcador(24, 23), partido.obtenerMarcadorDelSetActual());

        // Y el juego puede seguir en el mismo set
        partido.anotarPunto(new IdEvento("evt_p41"), Equipo.VISITANTE);
        assertEquals(new Marcador(24, 24), partido.obtenerMarcadorDelSetActual());
    }

    private void anotarVariosPuntos(PartidoVoleibol partido, Equipo equipo, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            partido.anotarPunto(new IdEvento("evt_" + equipo + "_" + i), equipo);
        }
    }
}
