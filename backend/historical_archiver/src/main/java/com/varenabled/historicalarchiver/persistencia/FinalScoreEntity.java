package com.varenabled.historicalarchiver.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "final_score")
public class FinalScoreEntity {

    @Id
    private String matchId;

    private int golesLocal;
    private int golesVisitante;

    protected FinalScoreEntity() {
    }

    public FinalScoreEntity(String matchId, int golesLocal, int golesVisitante) {
        this.matchId = matchId;
        this.golesLocal = golesLocal;
        this.golesVisitante = golesVisitante;
    }

    public String getMatchId() {
        return matchId;
    }

    public int getGolesLocal() {
        return golesLocal;
    }

    public void setGolesLocal(int golesLocal) {
        this.golesLocal = golesLocal;
    }

    public int getGolesVisitante() {
        return golesVisitante;
    }

    public void setGolesVisitante(int golesVisitante) {
        this.golesVisitante = golesVisitante;
    }
}
