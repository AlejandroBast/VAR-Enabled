package com.varenabled.livefeed;

public record CrearEventoRequest(String eventType, String team, String annulsEventId,
                                  Integer minuto, Integer minutoExtra, Integer minutosAgregados) {
}
