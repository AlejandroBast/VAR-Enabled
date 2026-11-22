package com.varenabled.dominio.futbol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.PartidoDeportivo;
import com.varenabled.dominio.apuestas.EstadoApuestasPartido;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartidoFutbol extends PartidoDeportivo {

    /** Una jugada que cuenta para un equipo (gol o falta) y que el VAR puede anular. */
    private record Jugada(IdEvento idEvento, Equipo equipo) {
    }

    private final List<Jugada> golesActivos = new ArrayList<>();
    private final List<Jugada> faltasActivas = new ArrayList<>();
    private Marcador marcador = Marcador.enCero();
    // Se reutiliza Marcador para contar faltas: local = faltas del local, visitante = faltas del visitante.
    private Marcador faltas = Marcador.enCero();
    private int minutoActual = 0;
    private int minutoExtraActual = 0;
    private EstadoPartido estado = EstadoPartido.EN_JUEGO;
    private String equipoLocal;
    private String equipoVisitante;

    public PartidoFutbol(IdPartido id) {
        super(id);
    }

    public void nombrarEquipos(String equipoLocal, String equipoVisitante) {
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
    }

    public void anotarGol(IdEvento idEvento, Equipo equipo) {
        golesActivos.add(new Jugada(idEvento, equipo));
        marcador = marcador.sumarGolDe(equipo);
    }

    public void anularGolPorVar(IdEvento idEventoAnulado) {
        Jugada gol = buscar(golesActivos, idEventoAnulado)
                .orElseThrow(() -> new EventoInexistenteException(idEventoAnulado));

        golesActivos.remove(gol);
        marcador = marcador.restarGolDe(gol.equipo());
        estado = EstadoPartido.EN_JUEGO;
    }

    public void registrarFalta(IdEvento idEvento, Equipo equipo) {
        faltasActivas.add(new Jugada(idEvento, equipo));
        faltas = faltas.sumarPuntosDe(equipo, 1);
    }

    public void anularFaltaPorVar(IdEvento idEventoAnulado) {
        Jugada falta = buscar(faltasActivas, idEventoAnulado)
                .orElseThrow(() -> new EventoInexistenteException(idEventoAnulado));

        faltasActivas.remove(falta);
        faltas = faltas.restarPuntosDe(falta.equipo(), 1);
        estado = EstadoPartido.EN_JUEGO;
    }

    public void programar() {
        estado = EstadoPartido.POR_JUGAR;
    }

    public void iniciar() {
        estado = EstadoPartido.EN_JUEGO;
    }

    public void iniciarRevisionVar() {
        estado = EstadoPartido.EN_REVISION;
    }

    public void confirmarJugadaPorVar() {
        estado = EstadoPartido.EN_JUEGO;
    }

    public void irAlEntretiempo() {
        estado = EstadoPartido.ENTRETIEMPO;
    }

    public void finalizar() {
        estado = EstadoPartido.FINALIZADO;
    }

    /**
     * Las apuestas solo estan abiertas mientras el partido va 0-0.
     * - Por jugar: todavia no hay apuestas en vivo (null).
     * - Revision VAR: detenidas (si se anula el gol, puede volver al 0-0).
     * - Final, o cualquier gol en el marcador: cerradas.
     */
    public EstadoApuestasPartido obtenerApuestas() {
        return switch (estado) {
            case POR_JUGAR -> null;
            case EN_REVISION -> EstadoApuestasPartido.DETENIDAS;
            case FINALIZADO -> EstadoApuestasPartido.CERRADAS;
            default -> marcador.equals(Marcador.enCero()) ? EstadoApuestasPartido.ABIERTAS : EstadoApuestasPartido.CERRADAS;
        };
    }

    public void actualizarMinuto(int minuto, int minutoExtra) {
        this.minutoActual = minuto;
        this.minutoExtraActual = minutoExtra;
    }

    public Marcador obtenerMarcador() {
        return marcador;
    }

    public Marcador obtenerFaltas() {
        return faltas;
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

    public String obtenerEquipoLocal() {
        return equipoLocal;
    }

    public String obtenerEquipoVisitante() {
        return equipoVisitante;
    }

    private Optional<Jugada> buscar(List<Jugada> jugadas, IdEvento idEvento) {
        return jugadas.stream()
                .filter(jugada -> jugada.idEvento().equals(idEvento))
                .findFirst();
    }
}
