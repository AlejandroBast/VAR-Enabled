package com.varenabled.dashboardbackend;

/**
 * Lo que viaja por el WebSocket hacia el frontend.
 * tipo = "marcador" (datos es un ActualizacionMarcadorDto) o "evento" (datos es un AlertaPartidoDto).
 */
public record MensajeDashboard(String tipo, Object datos) {

    public static final String MARCADOR = "marcador";
    public static final String EVENTO = "evento";
}
