package com.varenabled.aplicacion.apuestas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroEstadoApuestasTest {

    @Test
    void laPrimeraVezQueSeAplicaUnEstadoCambiaAlgo() {
        RegistroEstadoApuestas registro = new RegistroEstadoApuestas();

        boolean cambio = registro.aplicar("123", EstadoApuestas.SUSPENDIDO);

        assertTrue(cambio);
    }

    @Test
    void repetirElMismoComandoNoCambiaNada() {
        RegistroEstadoApuestas registro = new RegistroEstadoApuestas();
        registro.aplicar("123", EstadoApuestas.SUSPENDIDO);

        boolean cambio = registro.aplicar("123", EstadoApuestas.SUSPENDIDO);

        assertFalse(cambio);
    }

    @Test
    void reanudarDespuesDeSuspenderSiCambia() {
        RegistroEstadoApuestas registro = new RegistroEstadoApuestas();
        registro.aplicar("123", EstadoApuestas.SUSPENDIDO);

        boolean cambio = registro.aplicar("123", EstadoApuestas.ABIERTO);

        assertTrue(cambio);
    }

    @Test
    void cadaPartidoTieneSuPropioEstado() {
        RegistroEstadoApuestas registro = new RegistroEstadoApuestas();
        registro.aplicar("123", EstadoApuestas.SUSPENDIDO);

        boolean cambio = registro.aplicar("456", EstadoApuestas.SUSPENDIDO);

        assertTrue(cambio);
    }
}
