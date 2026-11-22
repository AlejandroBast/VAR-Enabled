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
    private Integer points;
    private Instant timestamp;

    protected MatchEventLogEntity() {
    }

    public MatchEventLogEntity(String matchId, String sport, String eventType, String team,
                                String eventId, String annulsEventId, Integer points, Instant timestamp) {
        this.matchId = matchId;
        this.sport = sport;
        this.eventType = eventType;
        this.team = team;
        this.eventId = eventId;
        this.annulsEventId = annulsEventId;
        this.points = points;
        this.timestamp = timestamp;
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

    public Integer getPoints() {
        return points;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
