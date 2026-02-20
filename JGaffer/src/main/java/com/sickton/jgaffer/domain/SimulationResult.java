package com.sickton.jgaffer.domain;

import java.util.List;

/**
 * The output of a full match simulation from a given minute to full-time.
 *
 * <p>Contains all events that occurred, the final score, raw fidelity counts
 * (for cumulative accumulation across re-simulations), a derived fidelity %,
 * and a Tactical IQ rating label.</p>
 */
public class SimulationResult {
    private final List<MatchEvent> events;
    private final int finalHomeGoals;
    private final int finalAwayGoals;
    /** Number of phases where the user's tactic matched the engine's recommendation. */
    private final int matchingPhases;
    /** Total number of phases evaluated in this simulation segment. */
    private final int totalPhases;
    /** 0-100: percentage of phases where user tactic matched engine recommendation. */
    private final int tacticFidelityScore;
    /** Tactical IQ rating label derived purely from fidelity. */
    private final String managerRating;

    public SimulationResult(List<MatchEvent> events,
                            int finalHomeGoals,
                            int finalAwayGoals,
                            int matchingPhases,
                            int totalPhases,
                            int tacticFidelityScore,
                            String managerRating) {
        this.events = events;
        this.finalHomeGoals = finalHomeGoals;
        this.finalAwayGoals = finalAwayGoals;
        this.matchingPhases = matchingPhases;
        this.totalPhases = totalPhases;
        this.tacticFidelityScore = tacticFidelityScore;
        this.managerRating = managerRating;
    }

    public List<MatchEvent> getEvents()        { return events; }
    public int getFinalHomeGoals()             { return finalHomeGoals; }
    public int getFinalAwayGoals()             { return finalAwayGoals; }
    public int getMatchingPhases()             { return matchingPhases; }
    public int getTotalPhases()                { return totalPhases; }
    public int getTacticFidelityScore()        { return tacticFidelityScore; }
    public String getManagerRating()           { return managerRating; }
}
