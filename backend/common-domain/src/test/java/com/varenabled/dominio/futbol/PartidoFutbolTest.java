package com.varenabled.dominio.futbol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.apuestas.EstadoApuestasPartido;
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

    @Test
    void unPartidoProgramadoQuedaPorJugarHastaQueSeInicia() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.programar();
        assertEquals(EstadoPartido.POR_JUGAR, partido.obtenerEstado());

        partido.iniciar();
        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
    }

    @Test
    void lasFaltasSeCuentanPorEquipo() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.registrarFalta(new IdEvento("evt_f1"), Equipo.LOCAL);
        partido.registrarFalta(new IdEvento("evt_f2"), Equipo.VISITANTE);
        partido.registrarFalta(new IdEvento("evt_f3"), Equipo.VISITANTE);

        assertEquals(new Marcador(1, 2), partido.obtenerFaltas());
    }

    @Test
    void anularUnaFaltaLaDescuentaYTerminaLaRevision() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.registrarFalta(new IdEvento("evt_f1"), Equipo.LOCAL);
        partido.iniciarRevisionVar();

        partido.anularFaltaPorVar(new IdEvento("evt_f1"));

        assertEquals(Marcador.enCero(), partido.obtenerFaltas());
        assertEquals(EstadoPartido.EN_JUEGO, partido.obtenerEstado());
    }

    @Test
    void anularUnaFaltaQueNoExisteLanzaExcepcion() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        assertThrows(EventoInexistenteException.class, () -> partido.anularFaltaPorVar(new IdEvento("evt_f9")));
    }

    @Test
    void elPartidoGuardaLosNombresDeLosEquipos() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.nombrarEquipos("Nacional", "América");

        assertEquals("Nacional", partido.obtenerEquipoLocal());
        assertEquals("América", partido.obtenerEquipoVisitante());
    }

    @Test
    void lasApuestasSoloEstanAbiertasMientrasVaCeroACero() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.programar();
        assertEquals(null, partido.obtenerApuestas());

        partido.iniciar();
        assertEquals(EstadoApuestasPartido.ABIERTAS, partido.obtenerApuestas());

        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);
        assertEquals(EstadoApuestasPartido.CERRADAS, partido.obtenerApuestas());
    }

    @Test
    void conUnGolLasApuestasSiguenCerradasAunqueElVarConfirmeLaJugada() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.anotarGol(new IdEvento("evt_g1"), Equipo.LOCAL);

        partido.iniciarRevisionVar();
        assertEquals(EstadoApuestasPartido.DETENIDAS, partido.obtenerApuestas());

        partido.confirmarJugadaPorVar();
        assertEquals(EstadoApuestasPartido.CERRADAS, partido.obtenerApuestas());
    }

    @Test
    void siElVarAnulaElUnicoGolVuelveAlCeroACeroYLasApuestasSeAbren() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));
        partido.anotarGol(new IdEvento("evt_g1"), Equipo.VISITANTE);
        partido.iniciarRevisionVar();

        partido.anularGolPorVar(new IdEvento("evt_g1"));

        assertEquals(EstadoApuestasPartido.ABIERTAS, partido.obtenerApuestas());
    }

    @Test
    void alFinalLasApuestasQuedanCerradasAunqueVayaCeroACero() {
        PartidoFutbol partido = new PartidoFutbol(new IdPartido("123"));

        partido.finalizar();

        assertEquals(EstadoApuestasPartido.CERRADAS, partido.obtenerApuestas());
    }
}
