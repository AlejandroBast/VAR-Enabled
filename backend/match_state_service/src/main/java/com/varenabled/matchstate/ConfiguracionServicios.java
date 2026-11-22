package com.varenabled.matchstate;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.aplicacion.futbol.RepositorioPartidoFutbolPort;
import com.varenabled.aplicacion.futbol.ServicioMarcadorFutbol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionServicios {

    @Bean
    public ServicioMarcadorFutbol servicioMarcadorFutbol(RepositorioPartidoFutbolPort repositorio,
                                                           PublicadorMarcadorPort publicadorMarcador,
                                                           PublicadorAlertasPort publicadorAlertas) {
        return new ServicioMarcadorFutbol(repositorio, publicadorMarcador, publicadorAlertas);
    }
}
