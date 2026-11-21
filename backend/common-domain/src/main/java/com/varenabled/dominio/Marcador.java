package com.varenabled.dominio;

public record Marcador(int local, int visitante) {

    public Marcador {
        if (local < 0 || visitante < 0) {
            throw new IllegalArgumentException("El marcador no puede ser negativo");
        }
    }

    public static Marcador enCero() {
        return new Marcador(0, 0);
    }

    public Marcador sumarPuntosDe(Equipo equipo, int puntos) {
        return equipo == Equipo.LOCAL
                ? new Marcador(local + puntos, visitante)
                : new Marcador(local, visitante + puntos);
    }

    public Marcador restarPuntosDe(Equipo equipo, int puntos) {
        return equipo == Equipo.LOCAL
                ? new Marcador(local - puntos, visitante)
                : new Marcador(local, visitante - puntos);
    }

    public Marcador sumarGolDe(Equipo equipo) {
        return sumarPuntosDe(equipo, 1);
    }

    public Marcador restarGolDe(Equipo equipo) {
        return restarPuntosDe(equipo, 1);
    }
}
