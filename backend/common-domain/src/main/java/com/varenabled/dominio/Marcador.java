package com.varenabled.dominio;

public record Marcador(int golesLocal, int golesVisitante) {

    public Marcador {
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new IllegalArgumentException("El marcador no puede ser negativo");
        }
    }

    public static Marcador enCero() {
        return new Marcador(0, 0);
    }

    public Marcador sumarGolDe(Equipo equipo) {
        return equipo == Equipo.LOCAL
                ? new Marcador(golesLocal + 1, golesVisitante)
                : new Marcador(golesLocal, golesVisitante + 1);
    }

    public Marcador restarGolDe(Equipo equipo) {
        return equipo == Equipo.LOCAL
                ? new Marcador(golesLocal - 1, golesVisitante)
                : new Marcador(golesLocal, golesVisitante - 1);
    }
}
