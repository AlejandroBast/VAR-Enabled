package com.varenabled.aplicacion;

import com.varenabled.dominio.futbol.PartidoFutbol;

public interface PublicadorMarcadorPort {

    /** Publica como va el partido: marcador, minuto, estado, equipos y faltas. */
    void publicarMarcador(PartidoFutbol partido);
}
