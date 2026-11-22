package com.varenabled.livefeed;

/** No lleva minuto: el minuto lo pone el reloj del partido al momento de recibir el evento. */
public record CrearEventoRequest(String eventType, String team, String annulsEventId, Integer minutosAgregados) {
}
