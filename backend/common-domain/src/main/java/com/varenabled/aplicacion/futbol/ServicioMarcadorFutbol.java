package com.varenabled.aplicacion.futbol;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.futbol.PartidoFutbol;

public class ServicioMarcadorFutbol {

    private final RepositorioPartidoFutbolPort repositorio;
    private final PublicadorMarcadorPort publicadorMarcador;
    private final PublicadorAlertasPort publicadorAlertas;

    public ServicioMarcadorFutbol(RepositorioPartidoFutbolPort repositorio,
                                   PublicadorMarcadorPort publicadorMarcador,
                                   PublicadorAlertasPort publicadorAlertas) {
        this.repositorio = repositorio;
        this.publicadorMarcador = publicadorMarcador;
        this.publicadorAlertas = publicadorAlertas;
    }

    public void procesarGol(IdPartido idPartido, IdEvento idEvento, Equipo equipo) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anotarGol(idEvento, equipo);

        repositorio.guardar(partido);
        publicadorMarcador.publicarMarcador(idPartido, partido.obtenerMarcador());
        publicadorAlertas.publicarAlerta(idPartido, "¡GOL del equipo " + equipo + "!");
    }

    public void procesarGolAnulado(IdPartido idPartido, IdEvento idEventoAnulado) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anularGolPorVar(idEventoAnulado);

        repositorio.guardar(partido);
        publicadorMarcador.publicarMarcador(idPartido, partido.obtenerMarcador());
        publicadorAlertas.publicarAlerta(idPartido, "¡GOL ANULADO POR EL VAR!");
    }

    private PartidoFutbol obtenerOCrear(IdPartido idPartido) {
        return repositorio.buscarPorId(idPartido).orElseGet(() -> new PartidoFutbol(idPartido));
    }
}
