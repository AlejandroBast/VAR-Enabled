package com.varenabled.historicalarchiver.kafka;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.historicalarchiver.aplicacion.ServicioArchivador;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaEventosArchivador {

    private final ServicioArchivador servicio;

    public EscuchaEventosArchivador(ServicioArchivador servicio) {
        this.servicio = servicio;
    }

    @KafkaListener(topics = "match_events", groupId = "historical-archiver")
    public void escuchar(EventoPartidoDto evento) {
        servicio.archivar(evento);
    }
}
