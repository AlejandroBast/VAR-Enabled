package com.varenabled.dashboardbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varenabled.contratos.ActualizacionMarcadorDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscuchaMarcadorTest {

    @Mock
    private MarcadorWebSocketHandler handler;

    @Test
    void transmiteLaActualizacionDelMarcadorComoJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EscuchaMarcador listener = new EscuchaMarcador(handler, mapper);
        ActualizacionMarcadorDto actualizacion = new ActualizacionMarcadorDto("123", 1, 0);

        listener.escuchar(actualizacion);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(handler).transmitir(captor.capture());
        ActualizacionMarcadorDto reconstruido = mapper.readValue(captor.getValue(), ActualizacionMarcadorDto.class);
        assertEquals(actualizacion, reconstruido);
    }
}
