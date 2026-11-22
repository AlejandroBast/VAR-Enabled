package com.varenabled.historicalarchiver.infraestructura;

import com.varenabled.dominio.Equipo;
import com.varenabled.historicalarchiver.aplicacion.MarcadorFinalPort;
import com.varenabled.historicalarchiver.persistencia.FinalScoreEntity;
import com.varenabled.historicalarchiver.persistencia.FinalScoreJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class MarcadorFinalJpaAdapter implements MarcadorFinalPort {

    private final FinalScoreJpaRepository repositorio;

    public MarcadorFinalJpaAdapter(FinalScoreJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void sumar(String matchId, Equipo equipo) {
        ajustar(matchId, equipo, 1);
    }

    @Override
    public void restar(String matchId, Equipo equipo) {
        ajustar(matchId, equipo, -1);
    }

    private void ajustar(String matchId, Equipo equipo, int delta) {
        FinalScoreEntity entidad = repositorio.findById(matchId)
                .orElseGet(() -> new FinalScoreEntity(matchId, 0, 0));

        if (equipo == Equipo.LOCAL) {
            entidad.setGolesLocal(entidad.getGolesLocal() + delta);
        } else {
            entidad.setGolesVisitante(entidad.getGolesVisitante() + delta);
        }

        repositorio.save(entidad);
    }
}
