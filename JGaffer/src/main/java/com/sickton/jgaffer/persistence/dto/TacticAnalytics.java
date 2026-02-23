package com.sickton.jgaffer.persistence.dto;

public class TacticAnalytics {
    private final String startTactic;
    private final Long totalSims;
    private final Long wins;
    private final Long losses;
    private final Long draws;
    private final double winRate;

    public TacticAnalytics(String startTactic, Long totalSims,
                           Long wins, Long losses, Long draws,
                           double winRate) {
        this.startTactic = startTactic;
        this.totalSims   = totalSims;
        this.wins        = wins;
        this.losses      = losses;
        this.draws       = draws;
        this.winRate     = winRate;
    }

    public String getStartTactic() { return startTactic; }
    public Long   getTotal()       { return totalSims; }
    public Long   getWins()        { return wins; }
    public Long   getLosses()      { return losses; }
    public Long   getDraws()       { return draws; }
    public double getWinRate()     { return winRate; }
}
