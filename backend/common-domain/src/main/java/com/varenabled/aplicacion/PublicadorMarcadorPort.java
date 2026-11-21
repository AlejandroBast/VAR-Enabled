package com.varenabled.aplicacion;

import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;

public interface PublicadorMarcadorPort {

    void publicarMarcador(IdPartido idPartido, Marcador marcador);
}
