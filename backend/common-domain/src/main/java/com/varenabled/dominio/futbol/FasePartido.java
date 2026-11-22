package com.varenabled.dominio.futbol;

public enum FasePartido {
    PRIMER_TIEMPO,
    ENTRETIEMPO,
    SEGUNDO_TIEMPO,
    FINALIZADO;

    public boolean seEstaJugando() {
        return this == PRIMER_TIEMPO || this == SEGUNDO_TIEMPO;
    }
}
