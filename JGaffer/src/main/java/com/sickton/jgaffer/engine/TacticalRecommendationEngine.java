package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.exceptions.MatchTeamException;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.rules.late_minutes.LateTeamDrawing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamLosing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamWinning;

import java.util.ArrayList;
import java.util.List;

public class TacticalRecommendationEngine {

    private List<TacticalRule> rules;

    public TacticalRecommendationEngine() {
        rules = new ArrayList<TacticalRule>();
        rules.add(new LateTeamLosing());
        rules.add(new LateTeamWinning());
        rules.add(new LateTeamDrawing());
    }

    public Tactic recommend(MatchContext context, Team team)
    {
        for(TacticalRule rule : rules)
        {
            if(rule.applies(context,team))
                return rule.recommend(context,team);
        }
        throw new MatchTeamException("Error Recommending Tactic");
    }
}