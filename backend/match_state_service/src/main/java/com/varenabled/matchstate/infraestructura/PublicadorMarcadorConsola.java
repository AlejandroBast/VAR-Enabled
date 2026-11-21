package com.varenabled.matchstate.infraestructura;

import com.varenabled.aplicacion.PublicadorMarcadorPort;
import com.varenabled.dominio.IdPartido;
import com.varenabled.dominio.Marcador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicadorMarcadorConsola implements PublicadorMarcadorPort {

    private static final Logger log = LoggerFactory.getLogger(PublicadorMarcadorConsola.class);

    @Override
    public void publicarMarcador(IdPartido idPartido, Marcador marcador) {
        log.info("score.match_{} -> {}-{}", idPartido.valor(), marcador.local(), marcador.visitante());
    }
}
