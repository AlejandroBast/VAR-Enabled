package com.varenabled.dominio.futbol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidoFutbolTest {

    @Test
    void empiezaConElMarcadorEnCero() {
        // Arrange
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        // Act
        Marcador marcador = partido.obtenerMarcador();

        // Assert
        assertEquals(Marcador.enCero(), marcador);
    }

    @Test
    void anotarGolDelLocalAumentaElMarcador() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);

        assertEquals(new Marcador(1, 0), partido.obtenerMarcador());
    }

    @Test
    void anotarGolDelVisitanteAumentaElMarcador() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.anotarGol(new IdEvento("evt_g1"), Equipo.VISITANTE);

        assertEquals(new Marcador(0, 1), partido.obtenerMarcador());
    }

    @Test
    void anularGolPorVarRevierteElMarcador() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.anotarGol(new IdEvento("evt_g123"), Equipo.LOCAL);

        partido.anularGolPorVar(new IdEvento("evt_g123"));

        assertEquals(Marcador.enCero(), partido.obtenerMarcador());
    }

    @Test
    void anularUnGolQueNoExisteLanzaExcepcion() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        assertThrows(EventoInexistenteException.class,
                () -> partido.anularGolPorVar(new IdEvento("evt_fantasma")));
    }

    @Test
    void noSePuedeAnularElMismoGolDosVeces() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        IdEvento idGol = new IdEvento("evt_g123");
        partido.anotarGol(idGol, Equipo.LOCAL);
        partido.anularGolPorVar(idGol);

        assertThrows(EventoInexistenteException.class,
                () -> partido.anularGolPorVar(idGol));
    }

    @Test
    void anularUnGolNoAfectaLosDemas() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);
        partido.anotarGol(new IdEvento("evt_g2"), Equipo.LOCAL);

        partido.anularGolPorVar(new IdEvento("evt_g1"));

        assertEquals(new Marcador(1, 0), partido.obtenerMarcador());
    }

    @Test
    void empiezaEnJuego() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
    }

    @Test
    void iniciarRevisionVarPoneElPartidoEnRevision() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.iniciarRevisionVar();

        assertEquals(EstadoPartido.EN_REVISION, partido.obtenerEstado());
    }

    @Test
    void confirmarJugadaTerminaLaRevisionYVuelveAJuego() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.iniciarRevisionVar();

        partido.confirmarJugadaPorVar();

        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
    }

    @Test
    void anularUnGolTerminaLaRevisionYVuelveAJuego() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);
        partido.iniciarRevisionVar();

        partido.anularGolPorVar(new IdEvento("evt_g1"));

        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
    }

    @Test
    void finalizarDejaElPartidoTerminado() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.finalizar();

        assertEquals(EstadoPartido.FINALIZADO, partido.obtenerEstado());
    }
}
