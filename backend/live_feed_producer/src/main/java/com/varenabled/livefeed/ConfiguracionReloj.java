package com.varenabled.livefeed;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConfiguracionReloj {

    /** Se inyecta como bean para que las pruebas puedan usar un reloj que avanza a mano. */
    @Bean
    public Clock relojDelSistema() {
        return Clock.systemUTC();
    }
}
