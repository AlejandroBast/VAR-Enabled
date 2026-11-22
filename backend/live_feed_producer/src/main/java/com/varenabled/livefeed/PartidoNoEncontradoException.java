package com.varenabled.livefeed;

public class PartidoNoEncontradoException extends RuntimeException {

    public PartidoNoEncontradoException(String matchId) {
        super("No existe un partido creado con matchId=" + matchId);
    }
}
