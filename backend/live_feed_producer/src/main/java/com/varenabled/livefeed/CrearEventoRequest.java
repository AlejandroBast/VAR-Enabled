package com.varenabled.livefeed;

/**
 * No lleva minuto: el minuto lo pone el reloj del partido al momento de recibir el evento.
 * reviews_event_id: solo para VAR_CHECK (el gol o la falta a revisar).
 * annuls_event_id: opcional en GOAL_ANNULLED / FOUL_ANNULLED (por defecto, la jugada en revision).
 */
public record CrearEventoRequest(String eventType, String team, String annulsEventId, String reviewsEventId,
                                 Integer minutosAgregados) {
}
