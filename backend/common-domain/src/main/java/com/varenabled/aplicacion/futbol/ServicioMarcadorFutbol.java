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
    public void procesarCreacion(IdPartido idPartido, String equipoLocal, String equipoVisitante) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.programar();
        partido.nombrarEquipos(equipoLocal, equipoVisitante);

        repositorio.guardar(partido);
        publicadorMarcador.publicarMarcador(partido);
    }

    public void procesarInicio(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "MATCH_STARTED", null, null,
                minuto, minutoExtra, "¡Comienza el partido!"));
    }

    public void procesarEntretiempo(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.irAlEntretiempo();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "HALF_TIME", null, null,
                minuto, minutoExtra, "Entretiempo"));
    }

    public void procesarSegundoTiempo(IdPartido idPartido, IdEvento idEvento, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "SECOND_HALF_STARTED", null, null,
                minuto, minutoExtra, "¡Comienza el segundo tiempo!"));
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

    public void procesarFalta(IdPartido idPartido, IdEvento idEvento, Equipo equipo, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        if (equipo != null) {
            partido.registrarFalta(idEvento, equipo);
        }
        partido.actualizarMinuto(minuto, minutoExtra);

        String mensaje = equipo == null ? "Falta" : "Falta del equipo " + equipo;
        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "FOUL", equipo, null,
                minuto, minutoExtra, mensaje));
    }

    public void procesarFaltaAnulada(IdPartido idPartido, IdEvento idEvento, IdEvento idFaltaAnulada, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.anularFaltaPorVar(idFaltaAnulada);
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "FOUL_ANNULLED", null, idFaltaAnulada,
                minuto, minutoExtra, "Falta anulada por el VAR"));
    }

    /** tipoTarjeta: YELLOW_CARD o RED_CARD. */
    public void procesarTarjeta(IdPartido idPartido, IdEvento idEvento, String tipoTarjeta, Equipo equipo, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, minutoExtra);

        String color = "RED_CARD".equals(tipoTarjeta) ? "roja" : "amarilla";
        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, tipoTarjeta, equipo, null,
                minuto, minutoExtra, "Tarjeta " + color + " para el equipo " + equipo));
    }

    public void procesarRevisionVar(IdPartido idPartido, IdEvento idEvento, IdEvento idEventoRevisado, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.iniciarRevisionVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "VAR_CHECK", null, null,
                minuto, minutoExtra, "Revisión VAR en curso", idEventoRevisado));
    }

    public void procesarJugadaConfirmada(IdPartido idPartido, IdEvento idEvento, IdEvento idEventoRevisado, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.confirmarJugadaPorVar();
        partido.actualizarMinuto(minuto, minutoExtra);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "MATCH_CONTINUES", null, null,
                minuto, minutoExtra, "El VAR confirma la jugada, el partido sigue", idEventoRevisado));
    }

    public void procesarTiempoAnadido(IdPartido idPartido, IdEvento idEvento, int minuto, int minutosAgregados) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, 0);

        guardarYPublicar(partido, new AlertaPartido(idPartido, idEvento, "ADDED_TIME", null, null,
                minuto, 0, minutosAgregados == 1 ? "Se añade 1 minuto" : "Se añaden " + minutosAgregados + " minutos"));
    }

    /** El reloj avanzo (o llego un evento sin alerta): solo se mueve el minuto. */
    public void actualizarMinuto(IdPartido idPartido, int minuto, int minutoExtra) {
        PartidoFutbol partido = obtenerOCrear(idPartido);

        partido.actualizarMinuto(minuto, minutoExtra);

        repositorio.guardar(partido);
        publicadorMarcador.publicarMarcador(partido);
    }

    private void guardarYPublicar(PartidoFutbol partido, AlertaPartido alerta) {
        repositorio.guardar(partido);
        publicadorMarcador.publicarMarcador(partido);
        publicadorAlertas.publicarAlerta(alerta);
    }

    private PartidoFutbol obtenerOCrear(IdPartido idPartido) {
        return repositorio.buscarPorId(idPartido).orElseGet(() -> new PartidoFutbol(idPartido));
    }
}
