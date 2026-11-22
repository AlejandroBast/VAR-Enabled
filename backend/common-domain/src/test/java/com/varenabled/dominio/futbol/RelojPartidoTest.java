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

    private MinutoPartido primerTiempo(int minuto, int extra, int jugados) {
        return new MinutoPartido(minuto, extra, FasePartido.PRIMER_TIEMPO, jugados);
    }

    private MinutoPartido segundoTiempo(int minuto, int extra, int jugados) {
        return new MinutoPartido(minuto, extra, FasePartido.SEGUNDO_TIEMPO, jugados);
    }

    @Test
    void apenasEmpiezaElPartidoVaEnElMinutoUno() {
        assertEquals(primerTiempo(1, 0, 1), reloj.minutoEn(INICIO));
    }

    @Test
    void elMinutoAvanzaConElTiempoReal() {
        assertEquals(primerTiempo(1, 0, 1), reloj.minutoEn(aLos(0, 59)));
        assertEquals(primerTiempo(2, 0, 2), reloj.minutoEn(aLos(1, 0)));
        assertEquals(primerTiempo(23, 0, 23), reloj.minutoEn(aLos(22, 30)));
    }

    @Test
    void sinTiempoAnadidoDespuesDel45VieneElEntretiempo() {
        assertEquals(primerTiempo(45, 0, 45), reloj.minutoEn(aLos(44, 0)));
        assertEquals(new MinutoPartido(45, 0, FasePartido.ENTRETIEMPO, 45), reloj.minutoEn(aLos(45, 0)));
        assertEquals(new MinutoPartido(45, 0, FasePartido.ENTRETIEMPO, 45), reloj.minutoEn(aLos(70, 0)));
    }

    @Test
    void conTiempoAnadidoElRelojCuentaLosExtraAntesDelEntretiempo() {
        reloj.anunciarTiempoAnadido(aLos(44, 10), 3);

        assertEquals(primerTiempo(45, 1, 46), reloj.minutoEn(aLos(45, 0)));
        assertEquals(primerTiempo(45, 3, 48), reloj.minutoEn(aLos(47, 0)));
        assertEquals(FasePartido.ENTRETIEMPO, reloj.minutoEn(aLos(48, 0)).fase());
    }

    @Test
    void elSegundoTiempoArrancaEnEl46CuandoSeInicia() {
        reloj.iniciarSegundoTiempo(aLos(60, 0));

        assertEquals(segundoTiempo(46, 0, 46), reloj.minutoEn(aLos(60, 0)));
        assertEquals(segundoTiempo(50, 0, 50), reloj.minutoEn(aLos(64, 0)));
    }

    @Test
    void losMinutosJugadosSumanElTiempoAnadidoDelPrimerTiempo() {
        reloj.anunciarTiempoAnadido(aLos(44, 0), 2);
        reloj.iniciarSegundoTiempo(aLos(60, 0));

        assertEquals(segundoTiempo(46, 0, 48), reloj.minutoEn(aLos(60, 0)));
    }

    @Test
    void noSePuedeIniciarElSegundoTiempoSiNoEsEntretiempo() {
        assertThrows(ReglaPartidoException.class, () -> reloj.iniciarSegundoTiempo(aLos(20, 0)));
    }

    @Test
    void elPartidoTerminaAlCumplirElNoventaMasElTiempoAnadido() {
        reloj.iniciarSegundoTiempo(aLos(45, 0));
        reloj.anunciarTiempoAnadido(aLos(89, 0), 2);

        assertEquals(segundoTiempo(90, 2, 92), reloj.minutoEn(aLos(91, 0)));
        assertEquals(new MinutoPartido(90, 2, FasePartido.FINALIZADO, 92), reloj.minutoEn(aLos(92, 0)));
    }

    @Test
    void sinTiempoAnadidoElPartidoTerminaDespuesDelNoventa() {
        reloj.iniciarSegundoTiempo(aLos(45, 0));

        assertEquals(segundoTiempo(90, 0, 90), reloj.minutoEn(aLos(89, 59)));
        assertEquals(FasePartido.FINALIZADO, reloj.minutoEn(aLos(90, 0)).fase());
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
    void noSePuedeAnunciarTiempoAnadidoEnElEntretiempo() {
        assertThrows(MinutoInvalidoException.class, () -> reloj.anunciarTiempoAnadido(aLos(50, 0), 3));
    }
}
