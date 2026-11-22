package com.varenabled.historicalarchiver.aplicacion;

import com.varenabled.contratos.EventoPartidoDto;
import com.varenabled.dominio.Equipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioArchivadorTest {

    @Mock
    private RegistroEventosPort registroEventos;

    @Mock
    private RegistroRevisionVarPort registroRevisiones;

    @Mock
    private MarcadorFinalPort marcadorFinal;

    @InjectMocks
    private ServicioArchivador servicio;

    @Test
    void archivarUnGolLoGuardaYSumaAlMarcador() {
        EventoPartidoDto gol = new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 10, 0, null, Instant.now());

        servicio.archivar(gol);

        verify(registroEventos).guardar(gol);
        verify(marcadorFinal).sumar("123", Equipo.LOCAL);
        verify(registroRevisiones, never()).guardar(any());
    }

    @Test
    void archivarUnGolAnuladoRegistraLaRevisionYRestaDelMarcador() {
        Instant momentoDelGol = Instant.parse("2026-01-01T10:00:00Z");
        Instant momentoDeLaAnulacion = momentoDelGol.plusSeconds(20);
        EventoPartidoDto golOriginal = new EventoPartidoDto("123", "FOOTBALL", "GOAL", "home", "evt_g1", null, 10, 0, null, momentoDelGol);
        EventoPartidoDto golAnulado = new EventoPartidoDto("123", "FOOTBALL", "GOAL_ANNULLED", null, null, "evt_g1", 12, 0, null, momentoDeLaAnulacion);
        when(registroEventos.buscarPorEventId("evt_g1")).thenReturn(Optional.of(golOriginal));

        servicio.archivar(golAnulado);

        verify(registroEventos).guardar(golAnulado);
        verify(marcadorFinal).restar("123", Equipo.LOCAL);

        ArgumentCaptor<RevisionVarRegistrada> captor = ArgumentCaptor.forClass(RevisionVarRegistrada.class);
        verify(registroRevisiones).guardar(captor.capture());
        RevisionVarRegistrada revision = captor.getValue();
        assertEquals("123", revision.matchId());
        assertEquals("FOOTBALL", revision.deporte());
        assertEquals("VAR", revision.tipoRevision());
        assertEquals("evt_g1", revision.eventoAnulado());
        assertEquals(Duration.ofSeconds(20), revision.duracion());
    }

    @Test
    void archivarUnaAnulacionSinEncontrarElOriginalNoRegistraRevisionNiMarcador() {
        EventoPartidoDto golAnulado = new EventoPartidoDto("123", "FOOTBALL", "GOAL_ANNULLED", null, null, "evt_fantasma", 12, 0, null, Instant.now());
        when(registroEventos.buscarPorEventId("evt_fantasma")).thenReturn(Optional.empty());

        servicio.archivar(golAnulado);

        verify(registroEventos).guardar(golAnulado);
        verify(registroRevisiones, never()).guardar(any());
        verify(marcadorFinal, never()).restar(any(), any());
    }
}
