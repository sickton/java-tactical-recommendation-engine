package com.sickton.jgaffer.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_decisions")
public class MatchDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer teamId;
    private Integer opponentId;
    private Integer matchId;

    private Integer startMinute;
    private String gamePhase;

    private String engineTactic;
    private String userTactic;

    private Integer fidelity;

    private Integer finalHomeGoals;
    private Integer finalAwayGoals;

    private Integer goalDelta;

    private LocalDateTime createdAt;

    private String league;
    private String startTactic;
    private Integer matchingPhases;
    private Integer totalPhases;
    private String outcome;

    public void setLeague(String league) {
        this.league = league;
    }

    public void setStartTactic(String startTactic) {
        this.startTactic = startTactic;
    }

    public void setMatchingPhases(Integer matchingPhases) {
        this.matchingPhases = matchingPhases;
    }

    public void setTotalPhases(Integer totalPhases) {
        this.totalPhases = totalPhases;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getLeague() {
        return league;
    }

    public String getStartTactic() {
        return startTactic;
    }

    public Integer getMatchingPhases() {
        return matchingPhases;
    }

    public Integer getTotalPhases() {
        return totalPhases;
    }

    public String getOutcome() {
        return outcome;
    }

    public UUID getId() {
        return id;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public Integer getOpponentId() {
        return opponentId;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public void setOpponentId(Integer opponentId) {
        this.opponentId = opponentId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public void setStartMinute(Integer startMinute) {
        this.startMinute = startMinute;
    }

    public void setGamePhase(String gamePhase) {
        this.gamePhase = gamePhase;
    }

    public void setEngineTactic(String engineTactic) {
        this.engineTactic = engineTactic;
    }

    public void setUserTactic(String userTactic) {
        this.userTactic = userTactic;
    }

    public void setFidelity(Integer fidelity) {
        this.fidelity = fidelity;
    }

    public void setFinalHomeGoals(Integer finalHomeGoals) {
        this.finalHomeGoals = finalHomeGoals;
    }

    public void setFinalAwayGoals(Integer finalAwayGoals) {
        this.finalAwayGoals = finalAwayGoals;
    }

    public void setGoalDelta(Integer goalDelta) {
        this.goalDelta = goalDelta;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public String getGamePhase() {
        return gamePhase;
    }

    public String getEngineTactic() {
        return engineTactic;
    }

    public String getUserTactic() {
        return userTactic;
    }

    public Integer getFidelity() {
        return fidelity;
    }

    public Integer getFinalHomeGoals() {
        return finalHomeGoals;
    }

    public Integer getFinalAwayGoals() {
        return finalAwayGoals;
    }

    public Integer getGoalDelta() {
        return goalDelta;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
