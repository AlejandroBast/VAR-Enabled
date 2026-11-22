package com.varenabled.historicalarchiver.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "match_event_log")
public class MatchEventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchId;
    private String sport;
    private String eventType;
    private String team;
    private String eventId;
    private String annulsEventId;
    private Integer minuto;
    private Integer minutoExtra;
    private Integer minutosAgregados;
    private Instant timestamp;
    private String reviewsEventId;
    private String homeTeam;
    private String awayTeam;

    protected MatchEventLogEntity() {
    }

    public MatchEventLogEntity(String matchId, String sport, String eventType, String team,
                                String eventId, String annulsEventId, Integer minuto, Integer minutoExtra,
                                Integer minutosAgregados, Instant timestamp,
                                String reviewsEventId, String homeTeam, String awayTeam) {
        this.matchId = matchId;
        this.sport = sport;
        this.eventType = eventType;
        this.team = team;
        this.eventId = eventId;
        this.annulsEventId = annulsEventId;
        this.minuto = minuto;
        this.minutoExtra = minutoExtra;
        this.minutosAgregados = minutosAgregados;
        this.timestamp = timestamp;
        this.reviewsEventId = reviewsEventId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public Long getId() {
        return id;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getSport() {
        return sport;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTeam() {
        return team;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAnnulsEventId() {
        return annulsEventId;
    }

    public Integer getMinuto() {
        return minuto;
    }

    public Integer getMinutoExtra() {
        return minutoExtra;
    }

    public Integer getMinutosAgregados() {
        return minutosAgregados;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getReviewsEventId() {
        return reviewsEventId;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }
}
