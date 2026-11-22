package com.varenabled.historicalarchiver.kafka;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.historicalarchiver.aplicacion.ServicioArchivador;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EscuchaEventosArchivador {

    private static final String AVANCE_RELOJ = "CLOCK_TICK";

    private final ServicioArchivador servicio;

    public EscuchaEventosArchivador(ServicioArchivador servicio) {
        this.servicio = servicio;
    }

    @KafkaListener(topics = "match_events", groupId = "historical-archiver")
    public void escuchar(EventoPartidoDto evento) {
        // El avance del reloj no es algo que haya pasado en el partido: no se archiva.
        if (AVANCE_RELOJ.equals(evento.eventType())) {
            return;
        }
        servicio.archivar(evento);
    }
}
