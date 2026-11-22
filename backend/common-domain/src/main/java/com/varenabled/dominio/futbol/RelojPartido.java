package com.varenabled.dominio.futbol;

import java.time.Duration;
import java.time.Instant;

/**
 * Reloj de un partido en vivo: el minuto sale del tiempo real que paso desde el inicio,
 * nadie lo escribe a mano.
 *
 * Primer tiempo: 1..45, y 45+1..45+N si se anuncio tiempo anadido. Despues queda en
 * ENTRETIEMPO hasta que se inicia el segundo tiempo: 46..90, 90+1..90+N, y termina.
 */
public class RelojPartido {

    private static final int FIN_PRIMERA_MITAD = 45;
    private static final int FIN_SEGUNDA_MITAD = 90;
    private static final int MAXIMO_TIEMPO_ANADIDO = 10;

    private final Instant inicio;
    private final Duration duracionMinuto;
    private Instant inicioSegundoTiempo;
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
        int anadidoPrimera = tiempoAnadidoPrimeraMitad == null ? 0 : tiempoAnadidoPrimeraMitad;
        int anadidoSegunda = tiempoAnadidoSegundaMitad == null ? 0 : tiempoAnadidoSegundaMitad;
        int duracionPrimerTiempo = FIN_PRIMERA_MITAD + anadidoPrimera;

        if (inicioSegundoTiempo == null) {
            // En el primer minuto real va el minuto 1.
            int corrido = minutosDesde(inicio, ahora) + 1;
            if (corrido <= FIN_PRIMERA_MITAD) {
                return new MinutoPartido(corrido, 0, FasePartido.PRIMER_TIEMPO, corrido);
            }
            if (corrido <= duracionPrimerTiempo) {
                return new MinutoPartido(FIN_PRIMERA_MITAD, corrido - FIN_PRIMERA_MITAD, FasePartido.PRIMER_TIEMPO, corrido);
            }
            return new MinutoPartido(FIN_PRIMERA_MITAD, anadidoPrimera, FasePartido.ENTRETIEMPO, duracionPrimerTiempo);
        }

        // Segundo tiempo: arranca en el 46.
        int minuto = minutosDesde(inicioSegundoTiempo, ahora) + FIN_PRIMERA_MITAD + 1;
        if (minuto <= FIN_SEGUNDA_MITAD) {
            return new MinutoPartido(minuto, 0, FasePartido.SEGUNDO_TIEMPO, duracionPrimerTiempo + minuto - FIN_PRIMERA_MITAD);
        }
        int extra = minuto - FIN_SEGUNDA_MITAD;
        if (extra <= anadidoSegunda) {
            return new MinutoPartido(FIN_SEGUNDA_MITAD, extra, FasePartido.SEGUNDO_TIEMPO,
                    duracionPrimerTiempo + FIN_PRIMERA_MITAD + extra);
        }
        return new MinutoPartido(FIN_SEGUNDA_MITAD, anadidoSegunda, FasePartido.FINALIZADO,
                duracionPrimerTiempo + FIN_PRIMERA_MITAD + anadidoSegunda);
    }

    public void iniciarSegundoTiempo(Instant ahora) {
        if (minutoEn(ahora).fase() != FasePartido.ENTRETIEMPO) {
            throw new ReglaPartidoException("El segundo tiempo solo se puede iniciar durante el entretiempo");
        }
        inicioSegundoTiempo = ahora;
    }

    public void anunciarTiempoAnadido(Instant ahora, int minutosAgregados) {
        MinutoPartido actual = minutoEn(ahora);
        boolean enElUltimoMinuto = actual.fase().seEstaJugando() && actual.minutoExtra() == 0
                && (actual.minuto() == FIN_PRIMERA_MITAD || actual.minuto() == FIN_SEGUNDA_MITAD);
        if (!enElUltimoMinuto) {
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

    private int minutosDesde(Instant desde, Instant ahora) {
        return (int) (Duration.between(desde, ahora).toMillis() / duracionMinuto.toMillis());
    }
}
