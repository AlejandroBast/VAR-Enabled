package com.varenabled.dominio.futbol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelojPartidoTest {

    @Test
    void aceptaCualquierMinutoReglamentarioSinTiempoExtra() {
        RelojPartido reloj = new RelojPartido();

        assertDoesNotThrow(() -> reloj.validarMinuto(1, 0));
        assertDoesNotThrow(() -> reloj.validarMinuto(45, 0));
        assertDoesNotThrow(() -> reloj.validarMinuto(90, 0));
    }

    @Test
    void rechazaUnMinutoFueraDeLos90Reglamentarios() {
        RelojPartido reloj = new RelojPartido();

        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(0, 0));
        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(91, 0));
    }

    @Test
    void rechazaMinutoExtraSiNoEsEnElCierreDeUnaMitad() {
        RelojPartido reloj = new RelojPartido();

        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(30, 1));
    }

    @Test
    void rechazaMinutoExtraSiNoSeAnuncioTiempoAnadidoParaEsaMitad() {
        RelojPartido reloj = new RelojPartido();

        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(45, 1));
        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(90, 1));
    }

    @Test
    void aceptaMinutoExtraDentroDelTiempoAnadidoAnunciadoEnLaPrimeraMitad() {
        RelojPartido reloj = new RelojPartido();
        reloj.anunciarTiempoAnadido(45, 3);

        assertDoesNotThrow(() -> reloj.validarMinuto(45, 1));
        assertDoesNotThrow(() -> reloj.validarMinuto(45, 3));
    }

    @Test
    void rechazaMinutoExtraMasAlaDelTiempoAnadidoAnunciado() {
        RelojPartido reloj = new RelojPartido();
        reloj.anunciarTiempoAnadido(45, 3);

        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(45, 4));
    }

    @Test
    void elTiempoAnadidoAplicaPorSeparadoACadaMitad() {
        RelojPartido reloj = new RelojPartido();
        reloj.anunciarTiempoAnadido(45, 2);
        reloj.anunciarTiempoAnadido(90, 5);

        assertDoesNotThrow(() -> reloj.validarMinuto(45, 2));
        assertThrows(MinutoInvalidoException.class, () -> reloj.validarMinuto(45, 5));
        assertDoesNotThrow(() -> reloj.validarMinuto(90, 5));
    }

    @Test
    void soloSePuedeAnunciarElTiempoAnadidoEnElUltimoMinutoDeCadaMitad() {
        RelojPartido reloj = new RelojPartido();

        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(44, 3));
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(89, 3));
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(60, 3));
    }

    @Test
    void elTiempoAnadidoMaximoEsDiez() {
        RelojPartido reloj = new RelojPartido();

        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(45, 11));
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(45, 0));
        assertDoesNotThrow(() -> reloj.anunciarTiempoAnadido(45, 10));
    }

    @Test
    void noSePuedeAnunciarElTiempoAnadidoDeLaMismaMitadDosVeces() {
        RelojPartido reloj = new RelojPartido();
        reloj.anunciarTiempoAnadido(45, 2);

        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(45, 3));
    }
}
