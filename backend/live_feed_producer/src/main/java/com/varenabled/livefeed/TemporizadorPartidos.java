package com.varenabled.livefeed;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TemporizadorPartidos {

    private final GestorPartidos gestor;

    public TemporizadorPartidos(GestorPartidos gestor) {
        this.gestor = gestor;
    }

    @Scheduled(fixedRate = 1000)
    public void avanzar() {
        gestor.avanzarRelojes();
    }
}
