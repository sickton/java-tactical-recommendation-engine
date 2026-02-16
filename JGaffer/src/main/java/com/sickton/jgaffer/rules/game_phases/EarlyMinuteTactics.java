package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.utility.TacticMapper;

import java.util.Map;

/**
 * Tactical rule for the Early Minutes phase (minutes 0-15).
 *
 * <p>Applies conservative weight adjustments focused on establishing structure
 * and tempo. Winning teams consolidate control, drawing teams seek rhythm,
 * and losing teams push forward while maintaining shape.</p>
 *
 * @see TacticalRule
 * @see GamePhase#EARLY_MINUTES
 * @author sickton
 */
public class EarlyMinuteTactics extends TacticalRule {
    protected static final double ADJUST_ONE = 0.03;
    protected static final double ADJUST_TWO = 0.05;
    protected static final double ADJUST_THREE = 0.08;

    /**
     * {@inheritDoc}
     *
     * @param context the current match context containing the match minute
     * @param team    the team being evaluated
     * @return {@code true} if the current minute falls within the Early Minutes phase (0-15)
     */
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = super.checkGamePhase(context.getMinute());
        return phase == GamePhase.EARLY_MINUTES;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies conservative adjustments for the opening phase of the match.
     * Winning teams consolidate control, drawing teams seek rhythm,
     * and losing teams push forward while accepting defensive risk.</p>
     *
     * @param context  the current match context including score and team information
     * @param team     the team for which a tactic is being recommended
     * @param tacticMap the tactic lookup map keyed by {@link TacticKey}
     * @return the recommended {@link Tactic} for the early minutes phase
     * @throws IllegalArgumentException if the match situation is invalid or no tactic mapping exists
     */
    @Override
    public Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap) {
        TeamIntent teamIntent = team.getIntent();
        double attack = teamIntent.getAttack();
        double control =  teamIntent.getControl();
        double defense = teamIntent.getDefence();
        if(isTeamWinning(context,team)){
            //team is winning - it is early in the game
            //makes sense to slow down the pace of the game, grow more into the game
            //and then try to carve open opportunities.
            //Therefore, attack reduces by 0.03
            //team tries to control more, 0.08 addition in control
            //team becomes defensively better, therefore, 0.03 addition in defense
            attack -= ADJUST_ONE;
            control += ADJUST_THREE;
            defense += ADJUST_ONE;
        }
        else if(isTeamDrawing(context,team)){
            //team is drawing - it is early in the game
            //should try to establish control and get a fluid understanding of the game
            //hence 0.05 added to control. To look like a team trying to win
            //midfield needs to be tight and fluid, therefore more focus on control
            //and a minor deduction of 0.03 from attack and defense
            attack -= ADJUST_ONE;
            control += ADJUST_TWO;
            defense -= ADJUST_ONE;
        }
        else if(isTeamLosing(context,team)){
            //team is in a losing position,
            //so preferably control the game more and attack more.
            //Therefore, most intent addition to control and attack
            //0.08 addition to control, 0.05 addition to attack
            //and deduction of 0.05 from defence to be on the front foot
            //and accept defensive risk
            attack += ADJUST_TWO;
            control += ADJUST_THREE;
            defense -= ADJUST_TWO;
        }
        else
            throw new IllegalArgumentException("Invalid match situation provided");
        WeightCombination combo = adjustWeights(clamp(attack),clamp(control), clamp(defense));
        Style teamStyle = team.getSquad().getTeamStyle();
        TacticKey key = new TacticKey(teamStyle, combo, GamePhase.EARLY_MINUTES);
        TacticSuggestion suggestion = tacticMap.get(key);
        if(suggestion == null)
            throw new IllegalArgumentException("Invalid tactic situation present");
        return suggestion.getSuggestedTactic();
    }
}
