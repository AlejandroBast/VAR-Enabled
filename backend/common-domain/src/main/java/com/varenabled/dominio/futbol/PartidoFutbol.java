package com.varenabled.dominio.futbol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
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
    private int minutoActual = 0;
    private int minutoExtraActual = 0;
    private EstadoPartido estado = EstadoPartido.EN_JUEGO;

    public PartidoFutbol(IdPartido id) {
        super(id);
    }

    public void anotarGol(IdEvento idEvento, Equipo equipo) {
        golesActivos.add(new Gol(idEvento, equipo));
        marcador = marcador.sumarGolDe(equipo);
    }

    public void anularGolPorVar(IdEvento idEventoAnulado) {
        Gol gol = buscarGolActivo(idEventoAnulado)
                .orElseThrow(() -> new EventoInexistenteException(idEventoAnulado));

        golesActivos.remove(gol);
        marcador = marcador.restarGolDe(gol.equipo());
        estado = EstadoPartido.EN_JUEGO;
    }

    public void iniciarRevisionVar() {
        estado = EstadoPartido.EN_REVISION;
    }

    public void confirmarJugadaPorVar() {
        estado = EstadoPartido.EN_JUEGO;
    }

    public void finalizar() {
        estado = EstadoPartido.FINALIZADO;
    }

    public void actualizarMinuto(int minuto, int minutoExtra) {
        this.minutoActual = minuto;
        this.minutoExtraActual = minutoExtra;
    }

    public Marcador obtenerMarcador() {
        return marcador;
    }

    public int obtenerMinutoActual() {
        return minutoActual;
    }

    public int obtenerMinutoExtraActual() {
        return minutoExtraActual;
    }

    public EstadoPartido obtenerEstado() {
        return estado;
    }

    private Optional<Gol> buscarGolActivo(IdEvento idEvento) {
        return golesActivos.stream()
                .filter(gol -> gol.idEvento().equals(idEvento))
                .findFirst();
    }
}
