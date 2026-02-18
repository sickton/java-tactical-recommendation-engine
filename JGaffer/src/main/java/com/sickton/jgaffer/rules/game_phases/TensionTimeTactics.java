package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

/**
 * Tactical rule for the Tension Time phase (minutes 61-70).
 *
 * <p>Manages the volatile mid-second-half period where fatigue begins to influence
 * structure. Applies both stamina and adaptability scaling factors, with larger
 * base adjustments reflecting the increasing urgency of tactical decisions.</p>
 *
 * @see TacticalRule
 * @see GamePhase#TENSION_TIME
 * @author sickton
 */
public class TensionTimeTactics extends TacticalRule {
    protected static final double ADJUST_ONE   = 0.03;
    protected static final double ADJUST_TWO   = 0.05;
    protected static final double ADJUST_THREE = 0.08;

    protected static final double LOW_STAMINA_FACTOR    = 0.85;
    protected static final double MEDIUM_STAMINA_FACTOR = 1.00;
    protected static final double HIGH_STAMINA_FACTOR   = 1.05;

    protected static final double LOW_ADAPT_FACTOR    = 0.90;
    protected static final double MEDIUM_ADAPT_FACTOR = 1.00;
    protected static final double HIGH_ADAPT_FACTOR   = 1.05;

    /**
     * {@inheritDoc}
     *
     * @param context the current match context containing the match minute
     * @param team    the team being evaluated
     * @return {@code true} if the current minute falls within the Tension Time phase (61-70)
     */
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.TENSION_TIME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Manages the volatile mid-second-half period with larger base adjustments
     * reflecting increasing urgency. Applies both stamina and adaptability scaling
     * factors as fatigue begins to influence team structure.</p>
     *
     * @param context  the current match context including score and team information
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the tactic lookup map keyed by {@link TacticKey}
     * @return the recommended {@link Tactic} for the tension time phase
     * @throws IllegalArgumentException if the match situation is invalid or no tactic mapping exists
     */
    @Override
    public Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack  = intent.getAttack();
        double control = intent.getControl();
        double defence = intent.getDefence();
        double dAttack  = 0.0;
        double dControl = 0.0;
        double dDefence = 0.0;
        if (isTeamWinning(context, team)) {
            dAttack  -= ADJUST_TWO;
            dControl += ADJUST_TWO;
            dDefence += ADJUST_TWO;
        }
        else if (isTeamDrawing(context, team)) {
            dAttack  += ADJUST_TWO;
            dControl += ADJUST_TWO;
            dDefence -= ADJUST_ONE;
        }
        else if (isTeamLosing(context, team)) {
            dAttack  += ADJUST_THREE;
            dControl += ADJUST_THREE;
            dDefence -= ADJUST_TWO;
        }
        else {
            throw new IllegalArgumentException("Invalid match situation in tension time");
        }
        int goalDiff = getGoalDifference(context);
        if (goalDiff >= 2) {
            if (isTeamWinning(context, team)) {
                dAttack  -= ADJUST_ONE;
                dDefence += ADJUST_ONE;
            }
            else if (isTeamLosing(context, team)) {
                dAttack  += ADJUST_ONE;
                dDefence -= ADJUST_ONE;
            }
        }
        // Opponent-aware counter-adjustment: applied before stamina/adaptability scaling
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefence};
        applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_TENSION);
        dAttack = deltas[0]; dControl = deltas[1]; dDefence = deltas[2];
        double staminaFactor = switch (getTeamStamina(team)) {
            case HIGH -> HIGH_STAMINA_FACTOR;
            case MEDIUM -> MEDIUM_STAMINA_FACTOR;
            case LOW -> LOW_STAMINA_FACTOR;
        };
        dAttack  *= staminaFactor;
        dControl *= staminaFactor;
        dDefence *= staminaFactor;
        double adaptFactor = switch (getTeamAdaptability(team)) {
            case HIGH -> HIGH_ADAPT_FACTOR;
            case MEDIUM -> MEDIUM_ADAPT_FACTOR;
            case LOW -> LOW_ADAPT_FACTOR;
        };
        dControl *= adaptFactor;
        dAttack  *= adaptFactor;
        attack  += dAttack;
        control += dControl;
        defence += dDefence;
        WeightCombination combo = adjustWeights(clamp(attack), clamp(control), clamp(defence));
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), combo, GamePhase.TENSION_TIME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null) {
            throw new IllegalArgumentException("No tactic found for tension-time state");
        }
        return suggestion.getSuggestedTactic();
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack = intent.getAttack(), control = intent.getControl(), defence = intent.getDefence();
        double dAttack = 0.0, dControl = 0.0, dDefence = 0.0;
        if (isTeamWinning(context, team)) {
            dAttack -= ADJUST_TWO; dControl += ADJUST_TWO; dDefence += ADJUST_TWO;
        } else if (isTeamDrawing(context, team)) {
            dAttack += ADJUST_TWO; dControl += ADJUST_TWO; dDefence -= ADJUST_ONE;
        } else if (isTeamLosing(context, team)) {
            dAttack += ADJUST_THREE; dControl += ADJUST_THREE; dDefence -= ADJUST_TWO;
        } else {
            throw new IllegalArgumentException("Invalid match situation in tension time");
        }
        int goalDiff = getGoalDifference(context);
        if (goalDiff >= 2) {
            if (isTeamWinning(context, team)) { dAttack -= ADJUST_ONE; dDefence += ADJUST_ONE; }
            else if (isTeamLosing(context, team)) { dAttack += ADJUST_ONE; dDefence -= ADJUST_ONE; }
        }
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefence};
        applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_TENSION);
        dAttack = deltas[0]; dControl = deltas[1]; dDefence = deltas[2];
        double staminaFactor = switch (getTeamStamina(team)) {
            case HIGH -> HIGH_STAMINA_FACTOR; case MEDIUM -> MEDIUM_STAMINA_FACTOR; case LOW -> LOW_STAMINA_FACTOR;
        };
        dAttack *= staminaFactor; dControl *= staminaFactor; dDefence *= staminaFactor;
        double adaptFactor = switch (getTeamAdaptability(team)) {
            case HIGH -> HIGH_ADAPT_FACTOR; case MEDIUM -> MEDIUM_ADAPT_FACTOR; case LOW -> LOW_ADAPT_FACTOR;
        };
        dControl *= adaptFactor;
        dAttack  *= adaptFactor;
        attack += dAttack; control += dControl; defence += dDefence;
        double ca = clamp(attack), cc = clamp(control), cd = clamp(defence);
        int confidence = computeConfidence(ca, cc, cd);
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), adjustWeights(ca, cc, cd), GamePhase.TENSION_TIME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null)
            throw new IllegalArgumentException("No tactic found for tension-time state");
        Formation formation = suggestFormation(suggestion.getSuggestedTactic(), team.getFormation());
        return new TacticRecommendation(suggestion.getSuggestedTactic(), confidence, formation);
    }
}