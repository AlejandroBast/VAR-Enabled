package com.varenabled.dominio;

public class EventoInexistenteException extends RuntimeException {

    public EventoInexistenteException(IdEvento idEvento) {
        super("No existe un evento activo con el id " + idEvento.valor());
    }
}
