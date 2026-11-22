package com.varenabled.dashboardbackend;

import com.varenabled.contratos.ActualizacionMarcadorDto;
import com.varenabled.contratos.AlertaPartidoDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guarda en memoria el ultimo marcador y los eventos de cada partido,
 * para que un navegador que se conecta tarde vea el partido completo.
 */
@Component
public class EstadoDashboard {

    private final Map<String, ActualizacionMarcadorDto> marcadores = new LinkedHashMap<>();
    private final List<AlertaPartidoDto> eventos = new ArrayList<>();
    private final Set<String> idsEventosVistos = new HashSet<>();

    public synchronized void registrarMarcador(ActualizacionMarcadorDto marcador) {
        marcadores.put(marcador.matchId(), marcador);
    }

    /**
     * Devuelve false si el evento ya se habia registrado (por ejemplo, cuando
     * match_state_service se reinicia y vuelve a publicar todo desde el principio).
     */
    public synchronized boolean registrarEvento(AlertaPartidoDto evento) {
        if (evento.eventId() != null && !idsEventosVistos.add(evento.eventId())) {
            return false;
        }
        eventos.add(evento);
        return true;
    }

    public synchronized List<MensajeDashboard> mensajesActuales() {
        List<MensajeDashboard> mensajes = new ArrayList<>();
        marcadores.values().forEach(marcador -> mensajes.add(new MensajeDashboard(MensajeDashboard.MARCADOR, marcador)));
        eventos.forEach(evento -> mensajes.add(new MensajeDashboard(MensajeDashboard.EVENTO, evento)));
        return mensajes;
    }
}
