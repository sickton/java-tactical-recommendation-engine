package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;

import java.util.Map;

/**
 * Abstract base class for all game-phase-specific tactical rules.
 *
 * <p>Defines the contract that every game phase implementation must follow:
 * {@link #applies(MatchContext, Team)} to determine if this rule is active for the
 * current minute, and {@link #recommend(MatchContext, Team, Map)} to generate the
 * tactic recommendation.</p>
 *
 * <p>Provides shared utilities used across all game phases: minute-to-phase mapping,
 * intent classification, match state queries (winning/losing/drawing), goal difference
 * calculation, and value clamping.</p>
 *
 * @see com.sickton.jgaffer.engine.TacticalRecommendationEngine
 * @see GamePhase
 * @author sickton
 */
public abstract class TacticalRule {
     /** Thresholds for time phases in a football game.
     0-15 early minutes
     16-44 closing half
     45-50 half-time
     51-60 building phase
     61-70 tension time
     71-87 late game
     88+ stoppage time */
    protected static final int EARLY_MINUTE_THRESHOLD = 15;
    protected static final int FIRST_HALF_THRESHOLD = 45;
    protected static final int SECOND_HALF_THRESHOLD = 50;
    protected static final int ASSESSING_TIME = 60;
    protected static final int TENSION_TIME_THRESHOLD = 70;
    protected static final int STOPPAGE_TIME = 88;

    protected static final double LOW_INTENT_THRESHOLD = 0.33;
    protected static final double MEDIUM_INTENT_THRESHOLD = 0.66;
    protected static final double HIGH_INTENT_THRESHOLD = 1;

    /**
     * Determines whether this tactical rule applies to the current match context and team.
     *
     * @param context the current match context containing minute, score, and participating teams
     * @param team    the team being evaluated
     * @return {@code true} if this rule's game phase matches the current match minute
     */
    public abstract boolean applies(MatchContext context, Team team);

    /**
     * Generates a tactic recommendation for the given match context, team, and tactic lookup map.
     *
     * @param context  the current match context containing minute, score, and participating teams
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the complete mapping of {@link TacticKey} to {@link TacticSuggestion}
     * @return the recommended {@link Tactic} for the current game state
     * @throws IllegalArgumentException if the match situation is invalid or no tactic is found
     */
    public abstract Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap);

    /**
     * Converts raw attack, control, and defence weight values into discrete
     * {@link IntentRange} categories and bundles them into a {@link WeightCombination}.
     *
     * @param attack  the attack weight value, expected in the range [0.0, 1.0]
     * @param control the control weight value, expected in the range [0.0, 1.0]
     * @param defence the defence weight value, expected in the range [0.0, 1.0]
     * @return a {@link WeightCombination} containing the discretized intent levels
     */
    public WeightCombination adjustWeights(double attack, double control, double defence) {
        IntentRange attackIntent = getIntent(attack);
        IntentRange controlIntent = getIntent(control);
        IntentRange defenceIntent = getIntent(defence);

        return new WeightCombination(attackIntent, defenceIntent, controlIntent);
    }

    /**
     * Maps a continuous intent value to a discrete {@link IntentRange} category.
     *
     * @param intent the intent value to classify, must be in the range [0.0, 1.0]
     * @return the corresponding {@link IntentRange} (LOW, MEDIUM, or HIGH)
     * @throws IllegalArgumentException if the intent value is outside the valid range
     */
    public IntentRange getIntent(double intent) {
        if(intent >= 0.0 && intent <= LOW_INTENT_THRESHOLD)
            return IntentRange.LOW;
        else if(intent > LOW_INTENT_THRESHOLD && intent <= MEDIUM_INTENT_THRESHOLD)
            return IntentRange.MEDIUM;
        else if(intent > MEDIUM_INTENT_THRESHOLD && intent <= HIGH_INTENT_THRESHOLD)
            return IntentRange.HIGH;
        else
            throw new IllegalArgumentException("Invalid intent level");
    }

    /**
     * Determines the {@link GamePhase} for a given match minute.
     *
     * @param minute the current minute of the match (0 or greater)
     * @return the {@link GamePhase} corresponding to the given minute
     * @throws IllegalArgumentException if the minute does not fall within any defined phase
     */
    public GamePhase checkGamePhase(int minute) {
        if(minute >= 0 && minute <= EARLY_MINUTE_THRESHOLD)
            return GamePhase.EARLY_MINUTES;
        else if(minute > EARLY_MINUTE_THRESHOLD && minute < FIRST_HALF_THRESHOLD)
            return GamePhase.CLOSING_HALF;
        else if(minute >= FIRST_HALF_THRESHOLD && minute <= SECOND_HALF_THRESHOLD)
            return GamePhase.HALF_TIME;
        else if(minute > SECOND_HALF_THRESHOLD && minute <= ASSESSING_TIME)
            return GamePhase.BUILD_PHASE;
        else if(minute > ASSESSING_TIME && minute <= TENSION_TIME_THRESHOLD)
            return GamePhase.TENSION_TIME;
        else if(minute > TENSION_TIME_THRESHOLD && minute < STOPPAGE_TIME)
            return GamePhase.LATE_GAME;
        else if(minute >= STOPPAGE_TIME)
            return GamePhase.STOPPAGE_TIME;
        throw new IllegalArgumentException("Invalid minute");
    }

    /**
     * Checks whether the specified team is currently winning in the given match context.
     *
     * @param context the current match context containing the score and team information
     * @param t       the team to check
     * @return {@code true} if the team has scored more goals than the opponent
     * @throws IllegalArgumentException if the team is not participating in this match
     */
    public boolean isTeamWinning(MatchContext context, Team t) {
        if(t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() > context.getAwayGoals();
        else if(t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() > context.getHomeGoals();
        else
            throw new IllegalArgumentException("Invalid Team input. Team not playing this match");
    }

    /**
     * Checks whether the specified team is currently losing in the given match context.
     *
     * @param context the current match context containing the score and team information
     * @param t       the team to check
     * @return {@code true} if the team has scored fewer goals than the opponent
     * @throws IllegalArgumentException if the team is not participating in this match
     */
    public boolean isTeamLosing(MatchContext context, Team t) {
        if(t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() < context.getAwayGoals();
        else if(t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() < context.getHomeGoals();
        else
            throw new IllegalArgumentException("Invalid Team input. Team not playing this match");
    }

    /**
     * Checks whether the match is currently a draw for the specified team.
     *
     * @param context the current match context containing the score and team information
     * @param t       the team to check
     * @return {@code true} if both teams have scored the same number of goals
     * @throws IllegalArgumentException if the team is not participating in this match
     */
    public boolean isTeamDrawing(MatchContext context, Team t) {
        if(!t.getName().equalsIgnoreCase(context.getHome().getName()) &&  !t.getName().equalsIgnoreCase(context.getAway().getName()))
            throw new IllegalArgumentException("Invalid team. Not playing this match");
        return context.getHomeGoals() == context.getAwayGoals();
    }

    /**
     * Calculates the absolute goal difference in the current match.
     *
     * @param context the current match context containing the score
     * @return the absolute difference between home and away goals
     */
    public int getGoalDifference(MatchContext context) {
        return Math.abs(context.getHomeGoals() - context.getAwayGoals());
    }

    /**
     * Retrieves the stamina level of the given team.
     *
     * @param team the team whose stamina level is queried
     * @return the {@link StaminaLevel} of the team
     */
    public StaminaLevel getTeamStamina(Team team) {
        return team.getStaminaLevel();
    }

    /**
     * Retrieves the adaptability level of the given team.
     *
     * @param team the team whose adaptability level is queried
     * @return the {@link TeamAdaptability} of the team
     */
    public TeamAdaptability getTeamAdaptability(Team team) {
        return team.getAdaptabilityLevel();
    }

    /**
     * Clamps a value to the range [0.0, 1.0].
     *
     * @param value the value to clamp
     * @return the clamped value, guaranteed to be between 0.0 and 1.0 inclusive
     */
    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}