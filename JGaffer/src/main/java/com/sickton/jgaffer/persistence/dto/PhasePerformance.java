package com.sickton.jgaffer.persistence.dto;

public record PhasePerformance(
        String phase,
        int totalSimulations,
        int wins,
        int losses,
        int draws,
        double winRate
) {}