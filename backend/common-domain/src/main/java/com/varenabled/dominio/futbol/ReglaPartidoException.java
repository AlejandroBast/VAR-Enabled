package com.varenabled.dominio.futbol;

/** Se lanza cuando un evento no se puede aplicar en el momento actual del partido (ej. revisar fuera de tiempo). */
public class ReglaPartidoException extends RuntimeException {

    public ReglaPartidoException(String mensaje) {
        super(mensaje);
    }
}
