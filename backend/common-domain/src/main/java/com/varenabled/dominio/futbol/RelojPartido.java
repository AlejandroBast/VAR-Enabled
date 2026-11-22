package com.varenabled.dominio.futbol;

public class RelojPartido {

    private static final int FIN_PRIMERA_MITAD = 45;
    private static final int FIN_SEGUNDA_MITAD = 90;
    private static final int MAXIMO_TIEMPO_ANADIDO = 10;

    private Integer tiempoAnadidoPrimeraMitad;
    private Integer tiempoAnadidoSegundaMitad;

    public void validarMinuto(int minuto, int minutoExtra) {
        if (minuto < 1 || minuto > FIN_SEGUNDA_MITAD) {
            throw new MinutoInvalidoException("El minuto debe estar entre 1 y " + FIN_SEGUNDA_MITAD);
        }
        if (minutoExtra == 0) {
            return;
        }
        if (minuto != FIN_PRIMERA_MITAD && minuto != FIN_SEGUNDA_MITAD) {
            throw new MinutoInvalidoException("El minuto extra solo aplica al cierre de una mitad (" + FIN_PRIMERA_MITAD + " o " + FIN_SEGUNDA_MITAD + ")");
        }

        Integer tiempoAnadido = minuto == FIN_PRIMERA_MITAD ? tiempoAnadidoPrimeraMitad : tiempoAnadidoSegundaMitad;
        if (tiempoAnadido == null || minutoExtra > tiempoAnadido) {
            throw new MinutoInvalidoException("No se anuncio tiempo anadido suficiente para el minuto " + minuto + "+" + minutoExtra);
        }
    }

    public void anunciarTiempoAnadido(int minuto, int minutosAgregados) {
        if (minuto != FIN_PRIMERA_MITAD && minuto != FIN_SEGUNDA_MITAD) {
            throw new MinutoInvalidoException("El tiempo anadido solo se puede anunciar en el ultimo minuto reglamentario de una mitad (" + FIN_PRIMERA_MITAD + " o " + FIN_SEGUNDA_MITAD + ")");
        }
        if (minutosAgregados < 1 || minutosAgregados > MAXIMO_TIEMPO_ANADIDO) {
            throw new MinutoInvalidoException("El tiempo anadido debe estar entre 1 y " + MAXIMO_TIEMPO_ANADIDO + " minutos");
        }

        if (minuto == FIN_PRIMERA_MITAD) {
            if (tiempoAnadidoPrimeraMitad != null) {
                throw new MinutoInvalidoException("Ya se anuncio el tiempo anadido de la primera mitad");
            }
            tiempoAnadidoPrimeraMitad = minutosAgregados;
        } else {
            if (tiempoAnadidoSegundaMitad != null) {
                throw new MinutoInvalidoException("Ya se anuncio el tiempo anadido de la segunda mitad");
            }
            tiempoAnadidoSegundaMitad = minutosAgregados;
        }
    }
}
