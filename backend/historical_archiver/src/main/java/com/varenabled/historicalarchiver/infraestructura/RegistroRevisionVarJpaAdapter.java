package com.varenabled.historicalarchiver.infraestructura;

import com.varenabled.historicalarchiver.aplicacion.RegistroRevisionVarPort;
import com.varenabled.historicalarchiver.aplicacion.RevisionVarRegistrada;
import com.varenabled.historicalarchiver.persistencia.VideoReviewAuditEntity;
import com.varenabled.historicalarchiver.persistencia.VideoReviewAuditJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class RegistroRevisionVarJpaAdapter implements RegistroRevisionVarPort {

    private final VideoReviewAuditJpaRepository repositorio;

    public RegistroRevisionVarJpaAdapter(VideoReviewAuditJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(RevisionVarRegistrada revision) {
        VideoReviewAuditEntity entidad = new VideoReviewAuditEntity(
                revision.matchId(),
                revision.deporte(),
                revision.tipoRevision(),
                revision.eventoAnulado(),
                revision.duracion().toMillis());

        repositorio.save(entidad);
    }
}
