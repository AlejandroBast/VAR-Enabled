package com.varenabled.dominio;

public record IdPartido(String valor) {

    public IdPartido {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id del partido no puede ser nulo ni vacio");
        }
    }
}
