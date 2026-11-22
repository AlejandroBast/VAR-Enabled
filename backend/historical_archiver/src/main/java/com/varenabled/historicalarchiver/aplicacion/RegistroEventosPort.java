package com.varenabled.historicalarchiver.aplicacion;

import com.varenabled.contratos.EventoPartidoDto;

import java.util.Optional;

public interface RegistroEventosPort {

    void guardar(EventoPartidoDto evento);

    Optional<EventoPartidoDto> buscarPorEventId(String eventId);
}
