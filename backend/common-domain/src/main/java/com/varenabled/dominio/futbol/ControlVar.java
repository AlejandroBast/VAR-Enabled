package com.varenabled.dominio.futbol;

import com.varenabled.dominio.IdEvento;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas del VAR de un partido:
 * - Un gol o una falta solo se puede revisar hasta 2 minutos despues de ocurrido
 *   (gol al 38' -> se puede revisar hasta el 40').
 * - Solo hay una revision abierta a la vez.
 * - La revision termina confirmando la jugada (MATCH_CONTINUES) o anulandola.
 * - Si pasan los 2 minutos sin revision, la jugada queda firme.
 */
public class ControlVar {

    public static final int MINUTOS_PARA_REVISAR = 2;

    public enum TipoJugada { GOL, FALTA }

    private record JugadaRevisable(IdEvento idEvento, TipoJugada tipo, int minutoJugado) {
    }

    private final Map<IdEvento, JugadaRevisable> enVentana = new LinkedHashMap<>();
    private JugadaRevisable enRevision;

    public void registrarJugada(IdEvento idEvento, TipoJugada tipo, int minutoJugado) {
        enVentana.put(idEvento, new JugadaRevisable(idEvento, tipo, minutoJugado));
    }

    public void abrirRevision(IdEvento idEvento, int minutoJugadoActual) {
        if (enRevision != null) {
            throw new ReglaPartidoException("Ya hay una revisión VAR abierta");
        }
        JugadaRevisable jugada = enVentana.get(idEvento);
        if (jugada == null) {
            throw new ReglaPartidoException("El evento " + idEvento.valor()
                    + " no se puede revisar: no es un gol o una falta vigente, o ya quedó firme");
        }
        if (minutoJugadoActual > jugada.minutoJugado() + MINUTOS_PARA_REVISAR) {
            throw new ReglaPartidoException("Ya pasaron más de " + MINUTOS_PARA_REVISAR
                    + " minutos desde la jugada: no se puede revisar");
        }
        enRevision = jugada;
    }

    public boolean hayRevisionAbierta() {
        return enRevision != null;
    }

    public IdEvento jugadaEnRevision() {
        return enRevision == null ? null : enRevision.idEvento();
    }

    public TipoJugada tipoEnRevision() {
        return enRevision == null ? null : enRevision.tipo();
    }

    /** El VAR revisó y la jugada se mantiene: queda firme. */
    public IdEvento confirmar() {
        if (enRevision == null) {
            throw new ReglaPartidoException("MATCH_CONTINUES solo se puede mandar si hay una revisión VAR abierta");
        }
        IdEvento confirmada = enRevision.idEvento();
        enVentana.remove(confirmada);
        enRevision = null;
        return confirmada;
    }

    /** El VAR revisó y anula la jugada: solo se puede anular la que está en revisión. */
    public void anular(IdEvento idEvento, TipoJugada tipo) {
        if (enRevision == null) {
            throw new ReglaPartidoException("Solo se puede anular una jugada que esté en revisión VAR");
        }
        if (!enRevision.idEvento().equals(idEvento) || enRevision.tipo() != tipo) {
            throw new ReglaPartidoException("La revisión abierta es de otro evento (" + enRevision.idEvento().valor() + ")");
        }
        enVentana.remove(idEvento);
        enRevision = null;
    }

    /**
     * Cierra las ventanas de las jugadas que ya no se pueden revisar y devuelve los goles que quedaron firmes.
     * La jugada que está en revisión no se toca.
     */
    public List<IdEvento> cerrarVentanasVencidas(int minutoJugadoActual) {
        return cerrarVentanas(minutoJugadoActual, false);
    }

    /** Al terminar un tiempo ya no se revisa nada: todo lo pendiente queda firme. */
    public List<IdEvento> cerrarTodasLasVentanas() {
        return cerrarVentanas(0, true);
    }

    private List<IdEvento> cerrarVentanas(int minutoJugadoActual, boolean todas) {
        List<IdEvento> golesFirmes = new ArrayList<>();
        Iterator<JugadaRevisable> it = enVentana.values().iterator();
        while (it.hasNext()) {
            JugadaRevisable jugada = it.next();
            boolean vencida = todas || minutoJugadoActual > jugada.minutoJugado() + MINUTOS_PARA_REVISAR;
            if (vencida && jugada != enRevision) {
                it.remove();
                if (jugada.tipo() == TipoJugada.GOL) {
                    golesFirmes.add(jugada.idEvento());
                }
            }
        }
        return golesFirmes;
    }
}
