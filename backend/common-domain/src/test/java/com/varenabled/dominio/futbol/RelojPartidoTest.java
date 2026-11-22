package com.varenabled.dominio.futbol;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelojPartidoTest {

    private static final Instant INICIO = Instant.parse("2026-11-22T20:00:00Z");

    // Para que las pruebas se lean facil: 1 minuto de partido = 1 minuto real.
    private final RelojPartido reloj = new RelojPartido(INICIO, Duration.ofMinutes(1));

    private Instant aLos(int minutosReales, int segundos) {
        return INICIO.plus(Duration.ofMinutes(minutosReales)).plusSeconds(segundos);
    }

    @Test
    void apenasEmpiezaElPartidoVaEnElMinutoUno() {
        assertEquals(new MinutoPartido(1, 0, false), reloj.minutoEn(INICIO));
    }

    @Test
    void elMinutoAvanzaConElTiempoReal() {
        assertEquals(new MinutoPartido(1, 0, false), reloj.minutoEn(aLos(0, 59)));
        assertEquals(new MinutoPartido(2, 0, false), reloj.minutoEn(aLos(1, 0)));
        assertEquals(new MinutoPartido(23, 0, false), reloj.minutoEn(aLos(22, 30)));
    }

    @Test
    void sinTiempoAnadidoDespuesDel45SigueEl46() {
        assertEquals(new MinutoPartido(45, 0, false), reloj.minutoEn(aLos(44, 0)));
        assertEquals(new MinutoPartido(46, 0, false), reloj.minutoEn(aLos(45, 0)));
    }

    @Test
    void conTiempoAnadidoElRelojSeQuedaEnEl45YCuentaLosExtra() {
        reloj.anunciarTiempoAnadido(aLos(44, 10), 3);

        assertEquals(new MinutoPartido(45, 1, false), reloj.minutoEn(aLos(45, 0)));
        assertEquals(new MinutoPartido(45, 3, false), reloj.minutoEn(aLos(47, 0)));
        assertEquals(new MinutoPartido(46, 0, false), reloj.minutoEn(aLos(48, 0)));
    }

    @Test
    void elPartidoTerminaAlCumplirElNoventaMasElTiempoAnadido() {
        reloj.anunciarTiempoAnadido(aLos(89, 0), 2);

        assertEquals(new MinutoPartido(90, 2, false), reloj.minutoEn(aLos(91, 0)));
        assertEquals(new MinutoPartido(90, 2, true), reloj.minutoEn(aLos(92, 0)));
    }

    @Test
    void sinTiempoAnadidoElPartidoTerminaDespuesDelNoventa() {
        assertEquals(new MinutoPartido(90, 0, false), reloj.minutoEn(aLos(89, 59)));
        assertEquals(new MinutoPartido(90, 0, true), reloj.minutoEn(aLos(90, 0)));
    }

    @Test
    void elTiempoAnadidoSoloSeAnunciaEnElUltimoMinutoDeUnaMitad() {
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(29, 0), 3));
    }

    @Test
    void elTiempoAnadidoDebeEstarEntreUnoYDiez() {
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(44, 0), 0));
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(44, 0), 11));
    }

    @Test
    void noSePuedeAnunciarDosVecesElTiempoAnadidoDeLaMismaMitad() {
        reloj.anunciarTiempoAnadido(aLos(44, 0), 2);

        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(44, 30), 3));
    }

    @Test
    void noSePuedeAnunciarTiempoAnadidoCuandoYaSeEstaJugando() {
        reloj.anunciarTiempoAnadido(aLos(44, 0), 2);

        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(45, 30), 3));
    }
}
