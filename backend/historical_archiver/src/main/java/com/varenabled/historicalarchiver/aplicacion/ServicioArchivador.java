package com.varenabled.historicalarchiver.aplicacion;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.Equipo;

import java.time.Duration;

public class ServicioArchivador {

    private final RegistroEventosPort registroEventos;
    private final RegistroRevisionVarPort registroRevisiones;
    private final MarcadorFinalPort marcadorFinal;

    public ServicioArchivador(RegistroEventosPort registroEventos,
                               RegistroRevisionVarPort registroRevisiones,
                               MarcadorFinalPort marcadorFinal) {
        this.registroEventos = registroEventos;
        this.registroRevisiones = registroRevisiones;
        this.marcadorFinal = marcadorFinal;
    }

    public void archivar(EventoPartidoDto evento) {
        registroEventos.guardar(evento);

        if (esAnulacion(evento.eventType())) {
            registroEventos.buscarPorEventId(evento.annulsEventId()).ifPresent(original -> {
                Duration duracion = Duration.between(original.timestamp(), evento.timestamp());
                registroRevisiones.guardar(new RevisionVarRegistrada(
                        evento.matchId(), evento.sport(), "VAR", evento.annulsEventId(), duracion));
                // Solo un gol anulado cambia el marcador final; una falta anulada no.
                if ("GOAL".equals(original.eventType())) {
                    marcadorFinal.restar(evento.matchId(), mapearEquipo(original.team()));
                }
            });
        } else if ("GOAL".equals(evento.eventType())) {
            marcadorFinal.sumar(evento.matchId(), mapearEquipo(evento.team()));
        }
    }

    private boolean esAnulacion(String eventType) {
        return eventType != null && eventType.endsWith("_ANNULLED");
    }

    private Equipo mapearEquipo(String team) {
        return "home".equals(team) ? Equipo.LOCAL : Equipo.VISITANTE;
    }
}
