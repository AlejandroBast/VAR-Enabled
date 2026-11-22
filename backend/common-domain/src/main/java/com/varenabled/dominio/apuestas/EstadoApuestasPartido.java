package com.varenabled.dominio.apuestas;

/** Como ve el publico las apuestas de un partido. */
public enum EstadoApuestasPartido {
    /** Se esta jugando y no hay nada pendiente. */
    ABIERTAS,
    /** Se anoto un gol (todavia se puede revisar) o termino el partido. */
    CERRADAS,
    /** El VAR esta revisando una jugada. */
    DETENIDAS
}
