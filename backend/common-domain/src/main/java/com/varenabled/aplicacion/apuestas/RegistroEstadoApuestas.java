package com.varenabled.aplicacion.apuestas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistroEstadoApuestas {

    private final Map<String, EstadoApuestas> estadosPorPartido = new ConcurrentHashMap<>();

    /**
     * Aplica el nuevo estado para el partido y devuelve true solo si realmente cambio algo,
     * de forma que un comando repetido (mismo estado de antes) no haga nada dos veces.
     */
    public boolean aplicar(String idPartido, EstadoApuestas nuevoEstado) {
        EstadoApuestas anterior = estadosPorPartido.put(idPartido, nuevoEstado);
        return anterior != nuevoEstado;
    }
}
