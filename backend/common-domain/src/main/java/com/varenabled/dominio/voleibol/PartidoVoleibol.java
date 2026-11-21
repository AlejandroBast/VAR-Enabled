package com.varenabled.dominio.voleibol;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.EventoInexistenteException;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import com.varenabled.dominio.PartidoDeportivo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PartidoVoleibol extends PartidoDeportivo {

    private static final int PUNTOS_PARA_GANAR_SET = 25;

    private record Punto(IdEvento idEvento, Equipo equipo) {
    }

    private final Map<Integer, List<Punto>> puntosPorSet = new HashMap<>();
    private final Map<Integer, Equipo> setsCerrados = new HashMap<>();
    private int numeroDeSetActual = 1;

    public PartidoVoleibol(IdPartido id) {
        super(id);
        puntosPorSet.put(numeroDeSetActual, new ArrayList<>());
    }

    public void anotarPunto(IdEvento idEvento, Equipo equipo) {
        List<Punto> puntosDelSet = puntosPorSet.get(numeroDeSetActual);
        puntosDelSet.add(new Punto(idEvento, equipo));

        Equipo ganadorDelSet = calcularGanador(puntosDelSet);
        if (ganadorDelSet != null) {
            setsCerrados.put(numeroDeSetActual, ganadorDelSet);
            numeroDeSetActual++;
            puntosPorSet.put(numeroDeSetActual, new ArrayList<>());
        }
    }

    public void anularPuntoPorChallenge(IdEvento idEventoAnulado) {
        for (Map.Entry<Integer, List<Punto>> set : puntosPorSet.entrySet()) {
            Optional<Punto> punto = buscarPunto(set.getValue(), idEventoAnulado);
            if (punto.isPresent()) {
                set.getValue().remove(punto.get());
                reabrirSetSiYaNoEstaGanado(set.getKey(), set.getValue());
                return;
            }
        }
        throw new EventoInexistenteException(idEventoAnulado);
    }

    public Marcador obtenerMarcadorDelSetActual() {
        return contarPuntos(puntosPorSet.get(numeroDeSetActual));
    }

    public int obtenerSetsGanadosLocal() {
        return contarSetsGanadosPor(Equipo.LOCAL);
    }

    public int obtenerSetsGanadosVisitante() {
        return contarSetsGanadosPor(Equipo.VISITANTE);
    }

    private void reabrirSetSiYaNoEstaGanado(int numeroDeSet, List<Punto> puntosDelSet) {
        boolean elSetEstabaCerrado = setsCerrados.containsKey(numeroDeSet);
        boolean sigueGanado = calcularGanador(puntosDelSet) != null;

        if (elSetEstabaCerrado && !sigueGanado) {
            setsCerrados.remove(numeroDeSet);
            numeroDeSetActual = numeroDeSet;
        }
    }

    private Equipo calcularGanador(List<Punto> puntosDelSet) {
        Marcador marcador = contarPuntos(puntosDelSet);
        if (marcador.local() >= PUNTOS_PARA_GANAR_SET) {
            return Equipo.LOCAL;
        }
        if (marcador.visitante() >= PUNTOS_PARA_GANAR_SET) {
            return Equipo.VISITANTE;
        }
        return null;
    }

    private Marcador contarPuntos(List<Punto> puntosDelSet) {
        int local = 0;
        int visitante = 0;
        for (Punto punto : puntosDelSet) {
            if (punto.equipo() == Equipo.LOCAL) {
                local++;
            } else {
                visitante++;
            }
        }
        return new Marcador(local, visitante);
    }

    private Optional<Punto> buscarPunto(List<Punto> puntosDelSet, IdEvento idEvento) {
        return puntosDelSet.stream()
                .filter(punto -> punto.idEvento().equals(idEvento))
                .findFirst();
    }

    private int contarSetsGanadosPor(Equipo equipo) {
        int total = 0;
        for (Equipo ganador : setsCerrados.values()) {
            if (ganador == equipo) {
                total++;
            }
        }
        return total;
    }
}
