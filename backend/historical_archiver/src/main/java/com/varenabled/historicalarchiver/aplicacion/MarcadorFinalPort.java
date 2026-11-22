package com.varenabled.historicalarchiver.aplicacion;

import com.varenabled.dominio.Equipo;

public interface MarcadorFinalPort {

    void sumar(String matchId, Equipo equipo);

    void restar(String matchId, Equipo equipo);
}
