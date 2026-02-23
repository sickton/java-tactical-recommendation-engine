package com.sickton.jgaffer.persistence.entity;

public interface TacticWinStats {
    String getStartTactic();
    Long getTotal();
    Long getWins();
    Long getLosses();
    Long getDraws();
}
