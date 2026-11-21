package com.varenabled.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarcadorTest {

    @Test
    void empiezaEnCero() {
        assertEquals(new Marcador(0, 0), Marcador.enCero());
    }

    @Test
    void noPermiteGolesLocalesNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new Marcador(-1, 0));
    }

    @Test
    void noPermiteGolesVisitantesNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new Marcador(0, -1));
    }

    @Test
    void sumarGolDelLocalAumentaSoloElLocal() {
        Marcador resultado = Marcador.enCero().sumarGolDe(Equipo.LOCAL);

        assertEquals(new Marcador(1, 0), resultado);
    }

    @Test
    void sumarGolDelVisitanteAumentaSoloElVisitante() {
        Marcador resultado = Marcador.enCero().sumarGolDe(Equipo.VISITANTE);

        assertEquals(new Marcador(0, 1), resultado);
    }

    @Test
    void restarGolDevuelveElMarcadorAnterior() {
        Marcador conUnGolLocal = new Marcador(1, 0);

        Marcador resultado = conUnGolLocal.restarGolDe(Equipo.LOCAL);

        assertEquals(Marcador.enCero(), resultado);
    }

    @Test
    void sumarPuntosDeUnaCanastaDeTresSumaLosTresAlLocal() {
        Marcador resultado = Marcador.enCero().sumarPuntosDe(Equipo.LOCAL, 3);

        assertEquals(new Marcador(3, 0), resultado);
    }

    @Test
    void restarPuntosDevuelveElMarcadorAnterior() {
        Marcador conUnaCanastaDeTres = new Marcador(0, 3);

        Marcador resultado = conUnaCanastaDeTres.restarPuntosDe(Equipo.VISITANTE, 3);

        assertEquals(Marcador.enCero(), resultado);
    }
}
