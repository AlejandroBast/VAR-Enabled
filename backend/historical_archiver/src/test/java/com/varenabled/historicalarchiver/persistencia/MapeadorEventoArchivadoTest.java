package com.varenabled.historicalarchiver.persistencia;

import com.varenabled.contratos.EventoPartidoDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapeadorEventoArchivadoTest {

    @Test
    void convierteElDtoAEntidadYDeVueltaSinPerderDatos() {
        Instant ahora = Instant.now();
        EventoPartidoDto original = new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, null, ahora);

        MatchEventLogEntity entidad = MapeadorEventoArchivado.aEntidad(original);
        EventoPartidoDto reconstruido = MapeadorEventoArchivado.aDto(entidad);

        assertEquals(original, reconstruido);
    }
}
