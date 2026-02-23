package com.sickton.jgaffer.persistence.dto;

public record LeagueNormalizerResult(
        String league,
        String tactic,
        int totalSimulations,
        int wins,
        int losses,
        int draws,
        double winRate
) {}