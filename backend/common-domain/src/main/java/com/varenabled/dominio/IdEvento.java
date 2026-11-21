package com.varenabled.dominio;

public record IdEvento(String valor) {

    public IdEvento {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id del evento no puede ser nulo ni vacio");
        }
    }
}
