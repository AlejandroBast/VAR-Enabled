package com.varenabled.dominio.apuestas;

public enum ComandoApuesta {
    SUSPENDER("SUSPEND_BETS"),
    REANUDAR("RESUME_BETS");

    private final String codigo;

    ComandoApuesta(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
