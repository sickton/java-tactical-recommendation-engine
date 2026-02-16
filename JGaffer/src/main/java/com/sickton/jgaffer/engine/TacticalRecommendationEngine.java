package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.rules.game_phases.*;
import com.sickton.jgaffer.utility.TacticMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central orchestrator that generates tactical recommendations for a given match context.
 *
 * <p>On construction, loads the tactic mapping from CSV via {@link TacticMapper} and
 * initializes all seven game phase {@link TacticalRule} implementations. When
 * {@link #recommendTactic(MatchContext, Team)} is called, it identifies the single
 * applicable game phase rule and delegates the recommendation to it.</p>
 *
 * <p>A defensive check ensures exactly one rule applies to any valid match minute.
 * If zero or multiple rules match, an {@link IllegalArgumentException} is thrown.</p>
 *
 * @see TacticalRule
 * @see MatchContext
 * @see TacticMapper
 * @author sickton
 */
public class TacticalRecommendationEngine {
    private final Map<TacticKey, TacticSuggestion> tacticMapping;
    private final List<TacticalRule> gamePhaseTactics;

    public TacticalRecommendationEngine() {
        tacticMapping = TacticMapper.mapTacticsAndWeights();
        gamePhaseTactics = initializeTactics();
    }

    private List<TacticalRule> initializeTactics() {
        List<TacticalRule> tactics = new ArrayList<>();
        //add instances of tactical rules, made based on phases in a game in that order
        //0-15 Early Minutes
        //16-44 Closing Half
        //45-50 HalfTime
        //51-60 BuildPhase
        //61-70 TensionTime
        //71-88 LateGame
        //88+ StoppageTime
        tactics.add(new EarlyMinuteTactics());
        tactics.add(new ClosingHalfTactics());
        tactics.add(new HalfTimeTactics());
        tactics.add(new BuildPhaseTactics());
        tactics.add(new TensionTimeTactics());
        tactics.add(new LateGameTactics());
        tactics.add(new StoppageTimeTactics());
        return tactics;
    }

    /**
     * Evaluates the current match context against all registered game-phase rules
     * and returns the recommended tactic from the single applicable rule.
     *
     * @param context the current match context including minute, score, and teams
     * @param team    the team for which a tactic recommendation is requested
     * @return the recommended {@link Tactic} for the given context and team
     * @throws IllegalArgumentException if zero or more than one rule applies to the
     *                                  current match minute, indicating an invalid match situation
     */
    public Tactic recommendTactic(MatchContext context, Team team) {
        int rules = 0;
        TacticalRule tacticRule = null;
        for(TacticalRule tacticalRule : gamePhaseTactics) {
            if(tacticalRule.applies(context, team)) {
                rules++;
                tacticRule = tacticalRule;
            }
        }
        if(rules != 1)
            throw new IllegalArgumentException("Invalid match situation, belongs to more than one phase of the game");
        else
            return tacticRule.recommend(context, team, tacticMapping);
    }
}