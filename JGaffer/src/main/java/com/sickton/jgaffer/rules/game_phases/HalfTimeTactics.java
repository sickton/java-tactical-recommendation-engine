package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

public class HalfTimeTactics extends TacticalRule {
    protected static final double GOAL_BIAS = 0.03;

    protected static final double ADJUST_ONE = 0.03;
    protected static final double ADJUST_TWO = 0.05;

    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.HALF_TIME;
    }

    @Override
    public Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack  = intent.getAttack();
        double control = intent.getControl();
        double defence = intent.getDefence();

        // Base halftime adjustments (strategic reset)
        if (isTeamWinning(context, team)) {
            attack  -= ADJUST_TWO;
            control += ADJUST_TWO;
            defence += ADJUST_TWO;
        }
        else if (isTeamDrawing(context, team)) {
            attack  += ADJUST_ONE;
            control += ADJUST_ONE;
            defence += ADJUST_ONE;
        }
        else if (isTeamLosing(context, team)) {
            attack  += ADJUST_TWO;
            control += ADJUST_TWO;
            defence -= ADJUST_ONE;
        }
        else {
            throw new IllegalArgumentException("Invalid match situation at halftime");
        }
        int goalDiff = getGoalDifference(context);
        if (goalDiff >= 2) {
            if (isTeamLosing(context, team)) {
                // Down by 2+ goals → come out more aggressive
                attack  += GOAL_BIAS;
                defence -= GOAL_BIAS;
            }
            else if (isTeamWinning(context, team)) {
                // Up by 2+ goals → protect structure
                attack  -= GOAL_BIAS;
                defence += GOAL_BIAS;
            }
        }
        WeightCombination combo = adjustWeights(clamp(attack), clamp(control), clamp(defence));
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), combo, GamePhase.HALF_TIME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null) {
            throw new IllegalArgumentException("No tactic found for halftime state");
        }
        return suggestion.getSuggestedTactic();
    }
}
