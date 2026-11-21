package com.varenabled.aplicacion.futbol;

import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.futbol.PartidoFutbol;

import java.util.Optional;

public interface RepositorioPartidoFutbolPort {

    Optional<PartidoFutbol> buscarPorId(IdPartido idPartido);

    void guardar(PartidoFutbol partido);
}
