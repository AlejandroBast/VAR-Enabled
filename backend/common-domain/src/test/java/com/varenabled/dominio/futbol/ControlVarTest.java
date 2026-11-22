package com.varenabled.dominio.futbol;

import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.futbol.ControlVar.TipoJugada;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlVarTest {

    private final ControlVar control = new ControlVar();
    private final IdEvento gol = new IdEvento("evt_g1");
    private final IdEvento falta = new IdEvento("evt_f1");

    @Test
    void unGolAl38SePuedeRevisarHastaEl40() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);

        control.abrirRevision(gol, 40);

        assertTrue(control.hayRevisionAbierta());
        assertEquals(gol, control.jugadaEnRevision());
    }

    @Test
    void unGolAl38YaNoSePuedeRevisarEnEl41() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);

        assertThrows(ReglaPartidoException.class, () -> control.abrirRevision(gol, 41));
    }

    @Test
    void lasFaltasSiguenLaMismaRegla() {
        control.registrarJugada(falta, TipoJugada.FALTA, 10);

        assertThrows(ReglaPartidoException.class, () -> control.abrirRevision(falta, 13));
    }

    @Test
    void noSePuedeRevisarUnEventoQueNoEsGolNiFalta() {
        assertThrows(ReglaPartidoException.class, () -> control.abrirRevision(new IdEvento("evt_x"), 5));
    }

    @Test
    void soloHayUnaRevisionALaVez() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);
        control.registrarJugada(falta, TipoJugada.FALTA, 38);
        control.abrirRevision(gol, 38);

        assertThrows(ReglaPartidoException.class, () -> control.abrirRevision(falta, 38));
    }

    @Test
    void confirmarCierraLaRevisionYLaJugadaQuedaFirme() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);
        control.abrirRevision(gol, 39);

        assertEquals(gol, control.confirmar());

        assertFalse(control.hayRevisionAbierta());
        assertThrows(ReglaPartidoException.class, () -> control.abrirRevision(gol, 39));
    }

    @Test
    void confirmarSinRevisionAbiertaNoSePuede() {
        assertThrows(ReglaPartidoException.class, control::confirmar);
    }

    @Test
    void soloSeAnulaLaJugadaQueEstaEnRevision() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);
        control.registrarJugada(falta, TipoJugada.FALTA, 38);
        control.abrirRevision(gol, 38);

        assertThrows(ReglaPartidoException.class, () -> control.anular(falta, TipoJugada.FALTA));

        control.anular(gol, TipoJugada.GOL);
        assertFalse(control.hayRevisionAbierta());
    }

    @Test
    void anularSinRevisionNoSePuede() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);

        assertThrows(ReglaPartidoException.class, () -> control.anular(gol, TipoJugada.GOL));
    }

    @Test
    void alVencerLaVentanaLosGolesQuedanFirmes() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);
        control.registrarJugada(falta, TipoJugada.FALTA, 38);

        assertEquals(List.of(), control.cerrarVentanasVencidas(40));
        assertEquals(List.of(gol), control.cerrarVentanasVencidas(41));
        assertEquals(List.of(), control.cerrarVentanasVencidas(42));
    }

    @Test
    void laJugadaEnRevisionNoVenceAunquePaseElTiempo() {
        control.registrarJugada(gol, TipoJugada.GOL, 38);
        control.abrirRevision(gol, 40);

        assertEquals(List.of(), control.cerrarVentanasVencidas(45));
        assertTrue(control.hayRevisionAbierta());
    }

    @Test
    void alTerminarElTiempoTodoLoPendienteQuedaFirme() {
        control.registrarJugada(gol, TipoJugada.GOL, 45);

        assertEquals(List.of(gol), control.cerrarTodasLasVentanas());
    }
}
