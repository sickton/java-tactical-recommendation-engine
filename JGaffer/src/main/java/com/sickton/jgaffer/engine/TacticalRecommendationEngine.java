package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.rules.game_phases.*;
import com.sickton.jgaffer.utility.TacticMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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