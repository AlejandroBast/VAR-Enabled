package com.varenabled.dominio.futbol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.GolInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.PartidoDeportivo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartidoFutbol extends PartidoDeportivo {

    private record Gol(IdEvento idEvento, Equipo equipo) {
    }

    private final List<Gol> golesActivos = new ArrayList<>();
    private Marcador marcador = Marcador.enCero();

    public PartidoFutbol(IdPartido id) {
        super(id);
    }

    public void anotarGol(IdEvento idEvento, Equipo equipo) {
        golesActivos.add(new Gol(idEvento, equipo));
        marcador = marcador.sumarGolDe(equipo);
    }

    public void anularGolPorVar(IdEvento idEventoAnulado) {
        Gol gol = buscarGolActivo(idEventoAnulado)
                .orElseThrow(() -> new GolInexistenteException(idEventoAnulado));

        golesActivos.remove(gol);
        marcador = marcador.restarGolDe(gol.equipo());
    }

    public Marcador obtenerMarcador() {
        return marcador;
    }

    private Optional<Gol> buscarGolActivo(IdEvento idEvento) {
        return golesActivos.stream()
                .filter(gol -> gol.idEvento().equals(idEvento))
                .findFirst();
    }
}
