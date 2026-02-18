package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

import java.util.Map;

/**
 * Tactical rule for the Closing Half phase (minutes 16-44).
 *
 * <p>Intensifies tactical adjustments as halftime approaches. Incorporates a
 * stamina factor that scales weight changes — low-stamina teams receive dampened
 * adjustments to conserve energy heading into the break.</p>
 *
 * @see TacticalRule
 * @see GamePhase#CLOSING_HALF
 * @author sickton
 */
public class ClosingHalfTactics extends TacticalRule {
    protected static final double ADJUST_ONE = 0.03;
    protected static final double ADJUST_TWO = 0.05;
    protected static final double ADJUST_THREE = 0.08;

    protected static final double LOW_STAMINA_FACTOR = 0.85;
    protected static final double MEDIUM_STAMINA_FACTOR = 1.00;
    protected static final double HIGH_STAMINA_FACTOR = 1.05;

    /**
     * {@inheritDoc}
     *
     * @param context the current match context containing the match minute
     * @param team    the team being evaluated
     * @return {@code true} if the current minute falls within the Closing Half phase (16-44)
     */
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = checkGamePhase(context.getMinute());
        return phase == GamePhase.CLOSING_HALF;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Intensifies tactical adjustments as halftime approaches, incorporating a
     * stamina factor that scales weight changes. Low-stamina teams receive dampened
     * adjustments to conserve energy heading into the break.</p>
     *
     * @param context  the current match context including score and team information
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the tactic lookup map keyed by {@link TacticKey}
     * @return the recommended {@link Tactic} for the closing half phase
     * @throws IllegalArgumentException if the match situation is invalid or no tactic mapping exists
     */
    @Override
    public Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack = intent.getAttack();
        double control = intent.getControl();
        double defense = intent.getDefence();
        double dAttack = 0.0;
        double dControl = 0.0;
        double dDefense = 0.0;
        double staminaFactor;
        switch (getTeamStamina(team)) {
            case HIGH -> staminaFactor = HIGH_STAMINA_FACTOR;
            case MEDIUM -> staminaFactor = MEDIUM_STAMINA_FACTOR;
            case LOW -> staminaFactor = LOW_STAMINA_FACTOR;
            default -> staminaFactor = 1.0;
        }
        if(isTeamWinning(context, team))
        {
            //team is winning and in the middle chunk of first half
            //should try to close out the half on maximum control and defense
            //therefore, should increase the control and defense by 0.08 and 0.05 respectively.
            dAttack -=  ADJUST_ONE;
            dDefense +=  ADJUST_TWO;
            dControl += ADJUST_THREE;
        }
        else if(isTeamDrawing(context, team))
        {
            //team is drawing in the minutes 16-44
            //focus should still be to close the half with momentum and control
            //consistent with modern day football shift to possession and momentum
            //therefore, team should strive on saving energy with control and focus
            //on holding shape.
            //Hence, decrease attack by 0.03, increase control by 0.05, and increase
            //defense by 0.03.
            dAttack  -= ADJUST_ONE;
            dControl += ADJUST_TWO;
            dDefense += ADJUST_ONE;
        }
        else if(isTeamLosing(context, team))
        {
            //team is losing in the first half
            dAttack  += ADJUST_THREE;
            dControl += ADJUST_TWO;
            dDefense -= ADJUST_TWO;
        }
        else
            throw new IllegalArgumentException("Invalid match situation provided");
        // Opponent-aware counter-adjustment: nudge deltas before applying stamina
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefense};
        applyOpponentStyleAdjustments(opponentStyle, deltas);
        dAttack = deltas[0]; dControl = deltas[1]; dDefense = deltas[2];
        attack += (dAttack * staminaFactor);
        control += (dControl * staminaFactor);
        defense += (dDefense * staminaFactor);
        WeightCombination wt = adjustWeights(clamp(attack), clamp(control), clamp(defense));
        Style teamStyle = team.getSquad().getTeamStyle();
        TacticKey key = new TacticKey(teamStyle, wt, GamePhase.CLOSING_HALF);
        TacticSuggestion suggestion = tacticMap.get(key);
        if(suggestion == null)
            throw new IllegalArgumentException("Invalid tactical situation provided");
        return suggestion.getSuggestedTactic();
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent intent = team.getIntent();
        double attack = intent.getAttack(), control = intent.getControl(), defense = intent.getDefence();
        double dAttack = 0.0, dControl = 0.0, dDefense = 0.0;
        double staminaFactor;
        switch (getTeamStamina(team)) {
            case HIGH -> staminaFactor = HIGH_STAMINA_FACTOR;
            case MEDIUM -> staminaFactor = MEDIUM_STAMINA_FACTOR;
            case LOW -> staminaFactor = LOW_STAMINA_FACTOR;
            default -> staminaFactor = 1.0;
        }
        if (isTeamWinning(context, team)) {
            dAttack -= ADJUST_ONE; dDefense += ADJUST_TWO; dControl += ADJUST_THREE;
        } else if (isTeamDrawing(context, team)) {
            dAttack -= ADJUST_ONE; dControl += ADJUST_TWO; dDefense += ADJUST_ONE;
        } else if (isTeamLosing(context, team)) {
            dAttack += ADJUST_THREE; dControl += ADJUST_TWO; dDefense -= ADJUST_TWO;
        } else {
            throw new IllegalArgumentException("Invalid match situation provided");
        }
        Style opponentStyle = getOpponent(context, team).getSquad().getTeamStyle();
        double[] deltas = {dAttack, dControl, dDefense};
        applyOpponentStyleAdjustments(opponentStyle, deltas);
        dAttack = deltas[0]; dControl = deltas[1]; dDefense = deltas[2];
        attack += (dAttack * staminaFactor);
        control += (dControl * staminaFactor);
        defense += (dDefense * staminaFactor);
        double ca = clamp(attack), cc = clamp(control), cd = clamp(defense);
        int confidence = computeConfidence(ca, cc, cd);
        TacticKey key = new TacticKey(team.getSquad().getTeamStyle(), adjustWeights(ca, cc, cd), GamePhase.CLOSING_HALF);
        TacticSuggestion suggestion = tacticMap.get(key);
        if (suggestion == null)
            throw new IllegalArgumentException("Invalid tactical situation provided");
        Formation formation = suggestFormation(suggestion.getSuggestedTactic(), team.getFormation());
        return new TacticRecommendation(suggestion.getSuggestedTactic(), confidence, formation);
    }
}
