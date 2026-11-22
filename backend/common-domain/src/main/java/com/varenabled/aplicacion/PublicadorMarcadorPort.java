package com.varenabled.aplicacion;

import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.futbol.EstadoPartido;

public interface PublicadorMarcadorPort {

    void publicarMarcador(IdPartido idPartido, Marcador marcador, int minuto, int minutoExtra, EstadoPartido estado);
}
