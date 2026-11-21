package com.varenabled.dominio;

public class GolInexistenteException extends RuntimeException {

    public GolInexistenteException(IdEvento idEvento) {
        super("No existe un gol activo con el id " + idEvento.valor());
    }
}
