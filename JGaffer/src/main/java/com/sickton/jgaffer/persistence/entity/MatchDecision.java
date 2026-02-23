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
}
