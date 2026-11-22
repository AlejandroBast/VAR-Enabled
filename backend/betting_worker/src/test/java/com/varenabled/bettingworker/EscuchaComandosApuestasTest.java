package com.varenabled.bettingworker;

import com.varenabled.aplicacion.apuestas.EstadoApuestas;
import com.varenabled.aplicacion.apuestas.RegistroEstadoApuestas;
import com.varenabled.contratos.ComandoApuestaDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscuchaComandosApuestasTest {

    @Mock
    private RegistroEstadoApuestas registro;

    @InjectMocks
    private EscuchaComandosApuestas listener;

    @Test
    void unComandoDeSuspensionSuspendeElPartido() {
        listener.escuchar(new ComandoApuestaDto("123", "SUSPEND_BETS"));

        verify(registro).aplicar("123", EstadoApuestas.SUSPENDIDO);
    }

    @Test
    void unComandoDeReanudacionAbreElPartido() {
        listener.escuchar(new ComandoApuestaDto("123", "RESUME_BETS"));

        verify(registro).aplicar("123", EstadoApuestas.ABIERTO);
    }
}
