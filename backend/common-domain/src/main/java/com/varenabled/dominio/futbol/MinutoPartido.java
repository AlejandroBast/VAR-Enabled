package com.varenabled.dominio.futbol;

/**
 * Lo que marca el reloj del partido en un momento: por ejemplo 45+2' es minuto 45, minutoExtra 2.
 *
 * minutosJugados cuenta todos los minutos corridos, incluido el tiempo anadido
 * (el 45+2' de una primera mitad con 3 anadidos es el minuto jugado 47). Sirve para medir
 * "2 minutos despues" sin pelear con el 45+N.
 */
public record MinutoPartido(int minuto, int minutoExtra, FasePartido fase, int minutosJugados) {
}
