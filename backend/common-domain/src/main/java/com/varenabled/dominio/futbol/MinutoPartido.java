package com.varenabled.dominio.futbol;

/**
 * Lo que marca el reloj del partido en un momento: por ejemplo 45+2' es minuto 45, minutoExtra 2.
 */
public record MinutoPartido(int minuto, int minutoExtra, boolean finalizado) {
}
