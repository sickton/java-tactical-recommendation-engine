package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

public class BuildPhaseTactics extends TacticalRule {
    protected static final double ADJUST_ONE = 0.03;
    protected static final double ADJUST_TWO = 0.05;

    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.BUILD_PHASE;
    }

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
            // Control the restart, avoid gifting momentum
            dAttack  -= ADJUST_ONE;
            dControl += ADJUST_TWO;
            dDefence += ADJUST_ONE;
        }
        else if (isTeamDrawing(context, team)) {
            // Assert second-half identity
            dAttack  += ADJUST_ONE;
            dControl += ADJUST_TWO;
            dDefence += ADJUST_ONE;
        }
        else if (isTeamLosing(context, team)) {
            // Start pushing, but remain structured
            dAttack  += ADJUST_TWO;
            dControl += ADJUST_TWO;
            dDefence -= ADJUST_ONE;
        }
        else {
            throw new IllegalArgumentException("Invalid match situation in build phase");
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
        double staminaFactor = switch (getTeamStamina(team)) {
            case HIGH -> 1.05;
            case MEDIUM -> 1.00;
            case LOW -> 0.85;
        };
        dAttack  *= staminaFactor;
        dControl *= staminaFactor;
        dDefence *= staminaFactor;
        double adaptabilityFactor = switch (getTeamAdaptability(team)) {
            case HIGH -> 1.05;
            case MEDIUM -> 1.00;
            case LOW -> 0.90;
        };
        dControl *= adaptabilityFactor;
        attack  += dAttack;
        control += dControl;
        defence += dDefence;
        WeightCombination combo = adjustWeights(clamp(attack), clamp(control), clamp(defence));
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), combo, GamePhase.BUILD_PHASE);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null) {
            throw new IllegalArgumentException("No tactic found for build-phase state");
        }
        return suggestion.getSuggestedTactic();
    }
}
