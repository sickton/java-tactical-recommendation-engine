package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;

import java.util.Map;


/**
 * Abstract base class for all game-phase-specific tactical rules.
 *
 * <p>Defines the contract that every game phase implementation must follow:
 * {@link #applies(MatchContext, Team)} to determine if this rule is active for the
 * current minute, and {@link #recommendWithConfidence(MatchContext, Team)} to generate
 * a recommendation with confidence score.</p>
 *
 * <p>Provides shared utilities used across all game phases: minute-to-phase mapping,
 * match state queries (winning/losing/drawing), goal difference calculation, and value clamping.</p>
 *
 * @see com.sickton.jgaffer.engine.TacticalRecommendationEngine
 * @see GamePhase
 * @author sickton
 */
public abstract class TacticalRule {

    /** Thresholds for time phases in a football game. */
    protected static final int EARLY_MINUTE_THRESHOLD = 15;
    protected static final int FIRST_HALF_THRESHOLD   = 45;
    protected static final int SECOND_HALF_THRESHOLD  = 50;
    protected static final int ASSESSING_TIME         = 60;
    protected static final int TENSION_TIME_THRESHOLD = 70;
    protected static final int STOPPAGE_TIME          = 88;

    /**
     * Determines whether this tactical rule applies to the current match context.
     *
     * @param context the current match context containing the match minute
     * @param team    the team being evaluated
     * @return {@code true} if this rule's game phase matches the current match minute
     */
    public abstract boolean applies(MatchContext context, Team team);

    /**
     * Generates a tactic recommendation paired with a confidence score and formation.
     *
     * @param context the current match context
     * @param team    the team for which a tactic is being recommended
     * @return a {@link TacticRecommendation} containing the recommended tactic and confidence (0–100)
     */
    public abstract TacticRecommendation recommendWithConfidence(MatchContext context, Team team);

    /**
     * Determines the {@link GamePhase} for a given match minute.
     *
     * @param minute the current minute of the match
     * @return the {@link GamePhase} corresponding to the given minute
     * @throws IllegalArgumentException if the minute does not fall within any defined phase
     */
    public GamePhase checkGamePhase(int minute) {
        if (minute >= 0 && minute <= EARLY_MINUTE_THRESHOLD)
            return GamePhase.EARLY_MINUTES;
        else if (minute > EARLY_MINUTE_THRESHOLD && minute < FIRST_HALF_THRESHOLD)
            return GamePhase.CLOSING_HALF;
        else if (minute >= FIRST_HALF_THRESHOLD && minute <= SECOND_HALF_THRESHOLD)
            return GamePhase.HALF_TIME;
        else if (minute > SECOND_HALF_THRESHOLD && minute <= ASSESSING_TIME)
            return GamePhase.BUILD_PHASE;
        else if (minute > ASSESSING_TIME && minute <= TENSION_TIME_THRESHOLD)
            return GamePhase.TENSION_TIME;
        else if (minute > TENSION_TIME_THRESHOLD && minute < STOPPAGE_TIME)
            return GamePhase.LATE_GAME;
        else if (minute >= STOPPAGE_TIME)
            return GamePhase.STOPPAGE_TIME;
        throw new IllegalArgumentException("Invalid minute: " + minute);
    }

    public boolean isTeamWinning(MatchContext context, Team t) {
        if (t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() > context.getAwayGoals();
        else if (t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() > context.getHomeGoals();
        throw new IllegalArgumentException("Team not playing this match");
    }

    public boolean isTeamLosing(MatchContext context, Team t) {
        if (t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() < context.getAwayGoals();
        else if (t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() < context.getHomeGoals();
        throw new IllegalArgumentException("Team not playing this match");
    }

    public boolean isTeamDrawing(MatchContext context, Team t) {
        if (!t.getName().equalsIgnoreCase(context.getHome().getName())
                && !t.getName().equalsIgnoreCase(context.getAway().getName()))
            throw new IllegalArgumentException("Team not playing this match");
        return context.getHomeGoals() == context.getAwayGoals();
    }

    public int getGoalDifference(MatchContext context) {
        return Math.abs(context.getHomeGoals() - context.getAwayGoals());
    }

    public StaminaLevel getTeamStamina(Team team) {
        return team.getStaminaLevel();
    }

    public TeamAdaptability getTeamAdaptability(Team team) {
        return team.getAdaptabilityLevel();
    }

    public Team getOpponent(MatchContext context, Team team) {
        if (team.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getAway();
        else if (team.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getHome();
        throw new IllegalArgumentException("Team is not playing in this match");
    }

    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
