package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

/**
 * Tactical rule for the Late Game phase (minutes 71-87).
 *
 * <p>Handles high-urgency tactical decisions heavily driven by the scoreline.
 * Uses a reduced low-stamina factor (0.80) compared to earlier phases, reflecting
 * the significant impact of fatigue at this stage. Both stamina and adaptability
 * scale the weight adjustments.</p>
 *
 * @see TacticalRule
 * @see GamePhase#LATE_GAME
 * @author sickton
 */
public class LateGameTactics extends TacticalRule {

    protected static final double ADJUST_ONE   = 0.03;
    protected static final double ADJUST_TWO   = 0.05;
    protected static final double ADJUST_THREE = 0.08;

    protected static final double LOW_STAMINA_FACTOR    = 0.80;
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
     * @return {@code true} if the current minute falls within the Late Game phase (71-87)
     */
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.LATE_GAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles high-urgency tactical decisions heavily driven by the scoreline.
     * Uses a reduced low-stamina factor compared to earlier phases, reflecting
     * the significant impact of fatigue at this stage.</p>
     *
     * @param context  the current match context including score and team information
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the tactic lookup map keyed by {@link TacticKey}
     * @return the recommended {@link Tactic} for the late game phase
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
            dDefence += ADJUST_THREE;
        } else if (isTeamDrawing(context, team)) {
            dAttack  += ADJUST_THREE;
            dControl += ADJUST_TWO;
            dDefence -= ADJUST_ONE;
        } else if (isTeamLosing(context, team)) {
            dAttack  += ADJUST_THREE;
            dControl += ADJUST_THREE;
            dDefence -= ADJUST_TWO;
        } else {
            throw new IllegalArgumentException("Invalid match situation in late game");
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
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), combo, GamePhase.LATE_GAME);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null) {
            throw new IllegalArgumentException("No tactic found for late-game state");
        }
        return suggestion.getSuggestedTactic();
    }
}
