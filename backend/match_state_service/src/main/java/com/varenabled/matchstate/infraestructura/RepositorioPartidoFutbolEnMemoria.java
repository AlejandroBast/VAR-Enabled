package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.futbol.RepositorioPartidoFutbolPort;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.futbol.PartidoFutbol;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RepositorioPartidoFutbolEnMemoria implements RepositorioPartidoFutbolPort {

    private final Map<IdPartido, PartidoFutbol> partidos = new ConcurrentHashMap<>();

    @Override
    public Optional<PartidoFutbol> buscarPorId(IdPartido idPartido) {
        return Optional.ofNullable(partidos.get(idPartido));
    }

    @Override
    public void guardar(PartidoFutbol partido) {
        partidos.put(partido.obtenerId(), partido);
    }
}
