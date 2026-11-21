package com.varenabled.dominio;

public abstract class PartidoDeportivo {

    private final IdPartido id;

    protected PartidoDeportivo(IdPartido id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del partido no puede ser nulo");
        }
        this.id = id;
    }

    public IdPartido obtenerId() {
        return id;
    }
}
