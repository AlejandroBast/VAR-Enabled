package com.varenabled.dominio.baloncesto;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.PartidoDeportivo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartidoBaloncesto extends PartidoDeportivo {

    private record Canasta(IdEvento idEvento, Equipo equipo, int puntos) {
    }

    private final List<Canasta> canastasActivas = new ArrayList<>();
    private Marcador marcador = Marcador.enCero();

    public PartidoBaloncesto(IdPartido id) {
        super(id);
    }

    public void anotarCanasta(IdEvento idEvento, Equipo equipo, int puntos) {
        if (puntos != 1 && puntos != 2 && puntos != 3) {
            throw new IllegalArgumentException("Una canasta vale 1, 2 o 3 puntos");
        }

        canastasActivas.add(new Canasta(idEvento, equipo, puntos));
        marcador = marcador.sumarPuntosDe(equipo, puntos);
    }

    public void anularCanastaPorReplay(IdEvento idEventoAnulado) {
        Canasta canasta = buscarCanastaActiva(idEventoAnulado)
                .orElseThrow(() -> new EventoInexistenteException(idEventoAnulado));

        canastasActivas.remove(canasta);
        marcador = marcador.restarPuntosDe(canasta.equipo(), canasta.puntos());
    }

    public Marcador obtenerMarcador() {
        return marcador;
    }

    private Optional<Canasta> buscarCanastaActiva(IdEvento idEvento) {
        return canastasActivas.stream()
                .filter(canasta -> canasta.idEvento().equals(idEvento))
                .findFirst();
    }
}
