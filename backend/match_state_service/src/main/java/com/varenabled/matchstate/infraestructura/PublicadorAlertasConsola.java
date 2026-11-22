package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorAlertasPort;
import com.varenabled.dominio.IdPartido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicadorAlertasConsola implements PublicadorAlertasPort {

    private static final Logger log = LoggerFactory.getLogger(PublicadorAlertasConsola.class);

    @Override
    public void publicarAlerta(IdPartido idPartido, String mensaje) {
        log.info("[alerta partido {}] {}", idPartido.valor(), mensaje);
    }
}
