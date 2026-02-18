package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

/**
 * Tactical rule for the Half Time phase (minutes 45-50).
 *
 * <p>Represents the structured strategic recalibration window. Applies a goal
 * difference bias on top of base adjustments — teams trailing by 2+ goals
 * come out more aggressive, while teams leading by 2+ protect their structure.</p>
 *
 * @see TacticalRule
 * @see GamePhase#HALF_TIME
 * @author sickton
 */
public class HalfTimeTactics extends TacticalRule {
    protected static final double GOAL_BIAS = 0.03;

    protected static final double ADJUST_ONE = 0.03;
    protected static final double ADJUST_TWO = 0.05;

    /**
     * {@inheritDoc}
     *
     * @param context the current match context containing the match minute
     * @param team    the team being evaluated
     * @return {@code true} if the current minute falls within the Half Time phase (45-50)
     */
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.HALF_TIME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies a strategic recalibration with goal-difference bias. Teams trailing
     * by 2+ goals come out more aggressive, while teams leading by 2+ protect
     * their structure.</p>
     *
     * @param context  the current match context including score and team information
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the tactic lookup map keyed by {@link TacticKey}
     * @return the recommended {@link Tactic} for the half time phase
     * @throws IllegalArgumentException if the match situation is invalid or no tactic mapping exists
     */
    @Override
    public Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack  = intent.getAttack();
        double control = intent.getControl();
        double defence = intent.getDefence();
        double dAttack = 0.0, dControl = 0.0, dDefence = 0.0;

        // Base halftime adjustments (strategic reset)
        if (isTeamWinning(context, team)) {
            dAttack  -= ADJUST_TWO;
            dControl += ADJUST_TWO;
            dDefence += ADJUST_TWO;
        }
        else if (isTeamDrawing(context, team)) {
            dAttack  += ADJUST_ONE;
            dControl += ADJUST_ONE;
            dDefence += ADJUST_ONE;
        }
        else if (isTeamLosing(context, team)) {
            dAttack  += ADJUST_TWO;
            dControl += ADJUST_TWO;
            dDefence -= ADJUST_ONE;
        }
        else {
            throw new IllegalArgumentException("Invalid match situation at halftime");
        }
        int goalDiff = getGoalDifference(context);
        if (goalDiff >= 2) {
            if (isTeamLosing(context, team)) {
                // Down by 2+ goals → come out more aggressive
                dAttack  += GOAL_BIAS;
                dDefence -= GOAL_BIAS;
            }
            else if (isTeamWinning(context, team)) {
                // Up by 2+ goals → protect structure
                dAttack  -= GOAL_BIAS;
                dDefence += GOAL_BIAS;
            }
        }
        // Opponent-aware counter-adjustment: halftime is the key tactical reset moment
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefence};
        applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_HALFTIME);
        attack  += deltas[0];
        control += deltas[1];
        defence += deltas[2];
        WeightCombination combo = adjustWeights(clamp(attack), clamp(control), clamp(defence));
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), combo, GamePhase.HALF_TIME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null) {
            throw new IllegalArgumentException("No tactic found for halftime state");
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
            dAttack += ADJUST_ONE; dControl += ADJUST_ONE; dDefence += ADJUST_ONE;
        } else if (isTeamLosing(context, team)) {
            dAttack += ADJUST_TWO; dControl += ADJUST_TWO; dDefence -= ADJUST_ONE;
        } else {
            throw new IllegalArgumentException("Invalid match situation at halftime");
        }
        int goalDiff = getGoalDifference(context);
        if (goalDiff >= 2) {
            if (isTeamLosing(context, team)) { dAttack += GOAL_BIAS; dDefence -= GOAL_BIAS; }
            else if (isTeamWinning(context, team)) { dAttack -= GOAL_BIAS; dDefence += GOAL_BIAS; }
        }
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefence};
        applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_HALFTIME);
        attack += deltas[0]; control += deltas[1]; defence += deltas[2];
        double ca = clamp(attack), cc = clamp(control), cd = clamp(defence);
        int confidence = computeConfidence(ca, cc, cd);
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), adjustWeights(ca, cc, cd), GamePhase.HALF_TIME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null)
            throw new IllegalArgumentException("No tactic found for halftime state");
        Formation formation = suggestFormation(suggestion.getSuggestedTactic(), team.getFormation());
        return new TacticRecommendation(suggestion.getSuggestedTactic(), confidence, formation);
    }
}
