package com.varenabled.historicalarchiver.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "video_review_audit")
public class VideoReviewAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchId;
    private String deporte;
    private String tipoRevision;
    private String eventoAnulado;
    private Long duracionMs;

    protected VideoReviewAuditEntity() {
    }

    public VideoReviewAuditEntity(String matchId, String deporte, String tipoRevision,
                                   String eventoAnulado, Long duracionMs) {
        this.matchId = matchId;
        this.deporte = deporte;
        this.tipoRevision = tipoRevision;
        this.eventoAnulado = eventoAnulado;
        this.duracionMs = duracionMs;
    }

    public Long getId() {
        return id;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getDeporte() {
        return deporte;
    }

    public String getTipoRevision() {
        return tipoRevision;
    }

    public String getEventoAnulado() {
        return eventoAnulado;
    }

    public Long getDuracionMs() {
        return duracionMs;
    }
}
