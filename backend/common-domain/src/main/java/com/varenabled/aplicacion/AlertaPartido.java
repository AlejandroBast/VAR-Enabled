package com.varenabled.aplicacion;

import com.varenabled.dominio.Equipo;
import com.varenabled.dominio.IdEvento;
import com.varenabled.dominio.IdPartido;

/**
 * Lo que paso en el partido, listo para avisar a los demas sistemas.
 * equipo, idEventoAnulado e idEventoRevisado pueden ser null cuando el evento no los tiene.
 */
public record AlertaPartido(
        IdPartido idPartido,
        IdEvento idEvento,
        String tipoEvento,
        Equipo equipo,
        IdEvento idEventoAnulado,
        int minuto,
        int minutoExtra,
        String mensaje,
        IdEvento idEventoRevisado
) {

    /** Forma corta, para las alertas que no se refieren a una revision. */
    public AlertaPartido(IdPartido idPartido, IdEvento idEvento, String tipoEvento, Equipo equipo,
                         IdEvento idEventoAnulado, int minuto, int minutoExtra, String mensaje) {
        this(idPartido, idEvento, tipoEvento, equipo, idEventoAnulado, minuto, minutoExtra, mensaje, null);
    }
}
