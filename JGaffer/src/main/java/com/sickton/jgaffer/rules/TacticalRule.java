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

    // Phase-scale multipliers for opponent style adjustments.
    // Larger phases = later in the match = opponent context matters more.
    protected static final double OPP_SCALE_EARLY    = 0.5;
    protected static final double OPP_SCALE_CLOSING  = 0.75;
    protected static final double OPP_SCALE_HALFTIME = 1.0;
    protected static final double OPP_SCALE_BUILD    = 1.0;
    protected static final double OPP_SCALE_TENSION  = 1.25;
    protected static final double OPP_SCALE_LATE     = 1.5;
    protected static final double OPP_SCALE_STOPPAGE = 2.0;

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
     * Generates a tactic recommendation paired with a confidence score.
     *
     * @param context  the current match context containing minute, score, and participating teams
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the complete mapping of {@link TacticKey} to {@link TacticSuggestion}
     * @return a {@link TacticRecommendation} containing the recommended tactic and confidence (0–100)
     * @throws IllegalArgumentException if the match situation is invalid or no tactic is found
     */
    public abstract TacticRecommendation recommendWithConfidence(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap);

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

    /**
     * Computes a confidence score (0–100) for the recommendation based on how far
     * each final weight value sits from its nearest classification boundary (0.33, 0.66).
     *
     * <p>A weight near a boundary is ambiguous — a small change would flip it into a
     * different {@link IntentRange}. The further all three weights are from any boundary,
     * the more confident the recommendation is.</p>
     *
     * <p>Calculation: for each weight, find its distance to the nearest boundary.
     * The maximum possible distance from a boundary within [0, 1] is 0.165 (midpoint
     * of each zone: LOW centre=0.165, MEDIUM centre=0.495, HIGH centre=0.83).
     * Average the three distances, normalize to [0, 1], then scale to 0–100.</p>
     *
     * @param attack  the final clamped attack weight
     * @param control the final clamped control weight
     * @param defence the final clamped defence weight
     * @return an integer confidence score from 0 (boundary) to 100 (zone centre)
     */
    public int computeConfidence(double attack, double control, double defence) {
        double maxDistFromBoundary = 0.165;
        double avgDist = (distFromNearestBoundary(attack)
                        + distFromNearestBoundary(control)
                        + distFromNearestBoundary(defence)) / 3.0;
        return (int) Math.round((avgDist / maxDistFromBoundary) * 100);
    }

    private double distFromNearestBoundary(double w) {
        double d1 = Math.abs(w - LOW_INTENT_THRESHOLD);
        double d2 = Math.abs(w - MEDIUM_INTENT_THRESHOLD);
        return Math.min(d1, d2);
    }

    /**
     * Returns the opponent team relative to the given team in this match context.
     *
     * @param context the current match context
     * @param team    the team whose opponent to retrieve
     * @return the opposing {@link Team} object
     * @throws IllegalArgumentException if the team is not playing in this match
     */
    public Team getOpponent(MatchContext context, Team team) {
        if (team.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getAway();
        else if (team.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getHome();
        else
            throw new IllegalArgumentException("Team is not playing in this match");
    }

    /**
     * Applies a small counter-adjustment to the weight deltas based on the opponent's playing style.
     *
     * <p>Models how a coaching staff adjusts their approach depending on who they face:</p>
     * <ul>
     *   <li><b>vs ATTACKING opponent</b>: reinforce defence (+0.03), trim attack (-0.03)</li>
     *   <li><b>vs DEFENSIVE opponent</b>: push attack up (+0.03), ease off defence (-0.03)</li>
     *   <li><b>vs CONTROLLING opponent</b>: boost both control (+0.03) and attack (+0.03)</li>
     * </ul>
     *
     * @param opponentStyle the opponent team's playing style
     * @param deltas        a double[3] array of {dAttack, dControl, dDefence} — modified in-place
     */
    public void applyOpponentStyleAdjustments(Style opponentStyle, double[] deltas, double phaseScale) {
        switch (opponentStyle) {
            case ATTACKING -> {
                deltas[0] -= 0.03 * phaseScale; // trim attack — cope with their high tempo
                deltas[2] += 0.03 * phaseScale; // shore up defence
            }
            case DEFENSIVE -> {
                deltas[0] += 0.03 * phaseScale; // commit more forward — break down the low block
                deltas[2] -= 0.03 * phaseScale; // ease off defence
            }
            case CONTROLLING -> {
                deltas[0] += 0.03 * phaseScale; // press high to disrupt their buildup
                deltas[1] += 0.03 * phaseScale; // contest possession
            }
        }
    }

    /**
     * Suggests the optimal formation for a given tactic.
     *
     * <p>Maps each tactic to the formation that best supports its execution.
     * The team's base formation is available for future refinement.</p>
     *
     * @param tactic the recommended tactic
     * @param base   the team's base formation (available for future use)
     * @return the {@link Formation} most suited to executing the given tactic
     */
    public Formation suggestFormation(Tactic tactic, Formation base) {
        return switch (tactic) {
            case HIGH_PRESS, GEGENPRESSING, TIKI_TAKA -> Formation.F_4_3_3;
            case CONTROL, COUNTER_ATTACK              -> Formation.F_4_2_3_1;
            case DIRECT_PLAY                          -> Formation.F_3_5_2;
            case LOW_BLOCK                            -> Formation.F_5_3_2;
        };
    }
}