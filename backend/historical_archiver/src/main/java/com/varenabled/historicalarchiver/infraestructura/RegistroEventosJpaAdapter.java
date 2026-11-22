package com.varenabled.historicalarchiver.infraestructura;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.historicalarchiver.aplicacion.RegistroEventosPort;
import com.varenabled.historicalarchiver.persistencia.MapeadorEventoArchivado;
import com.varenabled.historicalarchiver.persistencia.MatchEventLogJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RegistroEventosJpaAdapter implements RegistroEventosPort {

    private final MatchEventLogJpaRepository repositorio;

    public RegistroEventosJpaAdapter(MatchEventLogJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(EventoPartidoDto evento) {
        repositorio.save(MapeadorEventoArchivado.aEntidad(evento));
    }

    @Override
    public Optional<EventoPartidoDto> buscarPorEventId(String eventId) {
        return repositorio.findByEventId(eventId).map(MapeadorEventoArchivado::aDto);
    }
}
