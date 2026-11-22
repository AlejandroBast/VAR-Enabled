package com.varenabled.historicalarchiver;

import com.varenabled.historicalarchiver.aplicacion.MarcadorFinalPort;
import com.varenabled.historicalarchiver.aplicacion.RegistroEventosPort;
import com.varenabled.historicalarchiver.aplicacion.RegistroRevisionVarPort;
import com.varenabled.historicalarchiver.aplicacion.ServicioArchivador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionServicios {

    @Bean
    public ServicioArchivador servicioArchivador(RegistroEventosPort registroEventos,
                                                  RegistroRevisionVarPort registroRevisiones,
                                                  MarcadorFinalPort marcadorFinal) {
        return new ServicioArchivador(registroEventos, registroRevisiones, marcadorFinal);
    }
}
