package com.sickton.jgaffer.persistence.dto;

import com.sickton.jgaffer.domain.Tactic;

public class TacticAnalytics {
    private final String startTactic;
    private final Long totalSims;
    private final Long wins;
    private final double winRate;

    public TacticAnalytics(String startTactic, Long totalSims, Long wins, double winRate) {
        this.startTactic = startTactic;
        this.totalSims = totalSims;
        this.wins = wins;
        this.winRate = winRate;
    }

    public Long getTotal() {
        return totalSims;
    }

    public Long getWins() {
        return wins;
    }

    public double getWinRate() {
        return winRate;
    }

    public String getStartTactic() {
        return startTactic;
    }
}
