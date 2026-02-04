package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

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

    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.TENSION_TIME;
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
}