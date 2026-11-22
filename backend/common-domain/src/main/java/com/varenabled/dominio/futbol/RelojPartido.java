package com.varenabled.dominio.futbol;

import java.time.Duration;
import java.time.Instant;

/**
 * Reloj de un partido en vivo: el minuto sale del tiempo real que paso desde el inicio,
 * nadie lo escribe a mano.
 *
 * Orden del partido: 1..45, luego 45+1..45+N si se anuncio tiempo anadido,
 * luego 46..90, luego 90+1..90+N, y ahi termina.
 */
public class RelojPartido {

    private static final int FIN_PRIMERA_MITAD = 45;
    private static final int FIN_SEGUNDA_MITAD = 90;
    private static final int MAXIMO_TIEMPO_ANADIDO = 10;

    private final Instant inicio;
    private final Duration duracionMinuto;
    private Integer tiempoAnadidoPrimeraMitad;
    private Integer tiempoAnadidoSegundaMitad;

    public RelojPartido(Instant inicio, Duration duracionMinuto) {
        if (inicio == null) {
            throw new IllegalArgumentException("El inicio no puede ser nulo");
        }
        if (duracionMinuto == null || duracionMinuto.isZero() || duracionMinuto.isNegative()) {
            throw new IllegalArgumentException("La duracion de un minuto debe ser positiva");
        }
        this.inicio = inicio;
        this.duracionMinuto = duracionMinuto;
    }

    public MinutoPartido minutoEn(Instant ahora) {
        // Minutos "jugados" contando desde 1: en el primer minuto real va el minuto 1.
        long minutosCorridos = Duration.between(inicio, ahora).toMillis() / duracionMinuto.toMillis() + 1;
        int anadidoPrimera = tiempoAnadidoPrimeraMitad == null ? 0 : tiempoAnadidoPrimeraMitad;
        int anadidoSegunda = tiempoAnadidoSegundaMitad == null ? 0 : tiempoAnadidoSegundaMitad;

        if (minutosCorridos <= FIN_PRIMERA_MITAD) {
            return new MinutoPartido((int) minutosCorridos, 0, false);
        }
        if (minutosCorridos <= FIN_PRIMERA_MITAD + anadidoPrimera) {
            return new MinutoPartido(FIN_PRIMERA_MITAD, (int) (minutosCorridos - FIN_PRIMERA_MITAD), false);
        }

        long minutoSegundaMitad = minutosCorridos - anadidoPrimera;
        if (minutoSegundaMitad <= FIN_SEGUNDA_MITAD) {
            return new MinutoPartido((int) minutoSegundaMitad, 0, false);
        }
        if (minutoSegundaMitad <= FIN_SEGUNDA_MITAD + anadidoSegunda) {
            return new MinutoPartido(FIN_SEGUNDA_MITAD, (int) (minutoSegundaMitad - FIN_SEGUNDA_MITAD), false);
        }
        return new MinutoPartido(FIN_SEGUNDA_MITAD, anadidoSegunda, true);
    }

    public void anunciarTiempoAnadido(Instant ahora, int minutosAgregados) {
        MinutoPartido actual = minutoEn(ahora);
        boolean enElUltimoMinuto = actual.minutoExtra() == 0
                && (actual.minuto() == FIN_PRIMERA_MITAD || actual.minuto() == FIN_SEGUNDA_MITAD);
        if (!enElUltimoMinuto || actual.finalizado()) {
            throw new MinutoInvalidoException("El tiempo añadido solo se puede anunciar durante el minuto "
                    + FIN_PRIMERA_MITAD + " o el " + FIN_SEGUNDA_MITAD + " (ahora va el " + actual.minuto()
                    + (actual.minutoExtra() > 0 ? "+" + actual.minutoExtra() : "") + "')");
        }
        if (minutosAgregados < 1 || minutosAgregados > MAXIMO_TIEMPO_ANADIDO) {
            throw new MinutoInvalidoException("El tiempo añadido debe estar entre 1 y " + MAXIMO_TIEMPO_ANADIDO + " minutos");
        }

        if (actual.minuto() == FIN_PRIMERA_MITAD) {
            if (tiempoAnadidoPrimeraMitad != null) {
                throw new MinutoInvalidoException("Ya se anunció el tiempo añadido de la primera mitad");
            }
            tiempoAnadidoPrimeraMitad = minutosAgregados;
        } else {
            if (tiempoAnadidoSegundaMitad != null) {
                throw new MinutoInvalidoException("Ya se anunció el tiempo añadido de la segunda mitad");
            }
            tiempoAnadidoSegundaMitad = minutosAgregados;
        }
    }
}
