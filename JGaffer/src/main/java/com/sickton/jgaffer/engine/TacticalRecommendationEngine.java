package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.rules.game_phases.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Central orchestrator that generates tactical recommendations for a given match context.
 *
 * <p>On construction, initializes all seven game phase {@link TacticalRule} implementations.
 * When {@link #recommendWithDetails(MatchContext, Team)} is called, it identifies the single
 * applicable phase rule and delegates the recommendation to it.</p>
 *
 * @see TacticalRule
 * @see MatchContext
 * @author sickton
 */
public class TacticalRecommendationEngine {

    private final List<TacticalRule> gamePhaseTactics;

    public TacticalRecommendationEngine() {
        gamePhaseTactics = new ArrayList<>();
        gamePhaseTactics.add(new EarlyMinuteTactics());
        gamePhaseTactics.add(new ClosingHalfTactics());
        gamePhaseTactics.add(new HalfTimeTactics());
        gamePhaseTactics.add(new BuildPhaseTactics());
        gamePhaseTactics.add(new TensionTimeTactics());
        gamePhaseTactics.add(new LateGameTactics());
        gamePhaseTactics.add(new StoppageTimeTactics());
    }

    /**
     * Evaluates the current match context and returns a {@link TacticRecommendation}
     * containing the recommended tactic, confidence score, and suggested formation.
     *
     * @param context the current match context including minute, score, and teams
     * @param team    the team for which a tactic recommendation is requested
     * @return a {@link TacticRecommendation} with the tactic and its confidence score
     * @throws IllegalArgumentException if zero or more than one rule applies
     */
    public TacticRecommendation recommendWithDetails(MatchContext context, Team team) {
        int matches = 0;
        TacticalRule applicable = null;
        for (TacticalRule rule : gamePhaseTactics) {
            if (rule.applies(context, team)) {
                matches++;
                applicable = rule;
            }
        }
        if (matches != 1)
            throw new IllegalArgumentException("Expected exactly one applicable phase rule, found: " + matches);
        return applicable.recommendWithConfidence(context, team);
    }
}
