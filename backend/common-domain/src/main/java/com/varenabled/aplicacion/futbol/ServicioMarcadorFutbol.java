package com.varenabled.aplicacion.futbol;

import com.varenabled.aplicacion.AlertaPartido;
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

    /** El partido se creo pero no empezo: se publica 0-0 "por jugar", sin alerta. */
    public void procesarCreacion(IdPartido idPartido) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.programar();

        repositorio.guardar(partido);
        publicarMarcador(partido);
    }

    public void procesarInicio(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "MATCH_STARTED", null, null,
                minuto, minutoExtra, "¡Comienza el partido!"));
    }

    public void procesarFinal(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.finalizar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "MATCH_ENDED", null, null,
                minuto, minutoExtra, "Final del partido"));
    }

    public void procesarGol(IdPartido idPartido, IdEvento idEvento, Equipo equipo, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anotarGol(idEvento, equipo);
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "GOAL", equipo, null,
                minuto, minutoExtra, "¡GOL del equipo " + equipo + "!"));
    }

    public void procesarGolAnulado(IdPartido idPartido, IdEvento idEvento, IdEvento idEventoAnulado, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anularGolPorVar(idEventoAnulado);
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "GOAL_ANNULLED", null, idEventoAnulado,
                minuto, minutoExtra, "¡GOL ANULADO POR EL VAR!"));
    }

    public void procesarRevisionVar(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciarRevisionVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "VAR_CHECK", null, null,
                minuto, minutoExtra, "Revisión VAR en curso"));
    }

    public void procesarJugadaConfirmada(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.confirmarJugadaPorVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "MATCH_CONTINUES", null, null,
                minuto, minutoExtra, "El VAR confirma la jugada, el partido sigue"));
    }

    public void procesarFalta(IdPartido idPartido, IdEvento idEvento, Equipo equipo, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, minutoExtra);

        String mensaje = equipo == null ? "Falta" : "Falta del equipo " + equipo;
        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "FOUL", equipo, null,
                minuto, minutoExtra, mensaje));
    }

    public void procesarTiempoAnadido(IdPartido idPartido, IdEvento idEvento, int minuto, int minutosAgregados) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, 0);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "ADDED_TIME", null, null,
                minuto, 0, "Se añaden " + minutosAgregados + " minutos"));
    }

    public void actualizarMinuto(IdPartido idPartido, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicarMarcador(partido);
    }

    private void guardarYPublicar(PartidoFutbol partido, AlertaPartido alerta) {
        repositorio.guardar(partido);
        publicarMarcador(partido);
        publicadorAlertas.publicarAlerta(alerta);
    }

    private void publicarMarcador(PartidoFutbol partido) {
        publicadorMarcador.publicarMarcador(
                partido.obtenerId(), partido.obtenerMarcador(), partido.obtenerMinutoActual(), partido.obtenerMinutoExtraActual(),
                partido.obtenerEstado());
    }

    private PartidoFutbol obtenerOCrear(IdPartido idPartido) {
        return repositorio.buscarPorId(idPartido).orElseGet(() -> new PartidoFutbol(idPartido));
    }
}
