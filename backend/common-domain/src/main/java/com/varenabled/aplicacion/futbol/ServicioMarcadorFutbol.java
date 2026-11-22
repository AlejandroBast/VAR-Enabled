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

    public void procesarGol(IdPartido idPartido, IdEvento idEvento, Equipo equipo, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anotarGol(idEvento, equipo);
        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(idPartido, partido);
        publicadorAlertas.publicarAlerta(idPartido, idEvento, "¡GOL del equipo " + equipo + "!");
    }

    public void procesarGolAnulado(IdPartido idPartido, IdEvento idEventoAnulado, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anularGolPorVar(idEventoAnulado);
        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(idPartido, partido);
        publicadorAlertas.publicarAlerta(idPartido, idEventoAnulado, "¡GOL ANULADO POR EL VAR!");
    }

    public void procesarRevisionVar(IdPartido idPartido, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciarRevisionVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(idPartido, partido);
    }

    public void procesarJugadaConfirmada(IdPartido idPartido, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.confirmarJugadaPorVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(idPartido, partido);
    }

    public void actualizarMinuto(IdPartido idPartido, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(idPartido, partido);
    }

    private void publicarMarcador(IdPartido idPartido, PartidoFutbol partido) {
        publicadorMarcador.publicarMarcador(
                idPartido, partido.obtenerMarcador(), partido.obtenerMinutoActual(), partido.obtenerMinutoExtraActual(),
                partido.obtenerEstado());
    }

    private PartidoFutbol obtenerOCrear(IdPartido idPartido) {
        return repositorio.buscarPorId(idPartido).orElseGet(() -> new PartidoFutbol(idPartido));
    }
}
