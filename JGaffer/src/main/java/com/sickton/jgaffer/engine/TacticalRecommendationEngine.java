package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.exceptions.MatchTeamException;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamDrawing;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamLosing;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamWinning;
import com.sickton.jgaffer.rules.late_minutes.LateTeamDrawing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamLosing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamWinning;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamLosing;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamDrawing;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamWinning;
import com.sickton.jgaffer.rules.other_factors.RedCardRules;

import java.util.ArrayList;
import java.util.List;

public class TacticalRecommendationEngine {

    protected static final int EARLY_MINUTE = 20;
    protected static final int LATE_MINUTE = 70;

    private final List<TacticalRule> earlyRules;
    private final List<TacticalRule> lateRules;
    private final List<TacticalRule> middleRules;
    private final List<TacticalRule> miscRules;

    public TacticalRecommendationEngine() {
        earlyRules = new ArrayList<>();
        lateRules = new ArrayList<>();
        middleRules = new ArrayList<>();
        miscRules = new ArrayList<>();
        lateRules.add(new LateTeamLosing());
        lateRules.add(new LateTeamWinning());
        lateRules.add(new LateTeamDrawing());
        middleRules.add(new MiddleTeamWinning());
        middleRules.add(new MiddleTeamLosing());
        middleRules.add(new MiddleTeamDrawing());
        earlyRules.add(new EarlyTeamLosing());
        earlyRules.add(new EarlyTeamDrawing());
        earlyRules.add(new EarlyTeamWinning());
        miscRules.add(new RedCardRules());
    }

    public Tactic recommend(MatchContext context, Team team)
    {
        for(TacticalRule rule : miscRules)
        {
            if(rule.applies(context, team))
                return rule.recommend(context, team);
        }
        if(context.getMinute() <= EARLY_MINUTE)
        {
            for(TacticalRule rule : earlyRules)
            {
                if(rule.applies(context, team))
                    return rule.recommend(context, team);
            }
        }
        else if(context.getMinute() >= LATE_MINUTE)
        {
            for(TacticalRule rule : lateRules)
            {
                if(rule.applies(context, team))
                    return rule.recommend(context, team);
            }
        }
        else
        {
            for(TacticalRule rule : middleRules)
            {
                if(rule.applies(context, team))
                    return rule.recommend(context, team);
            }
        }
        throw new MatchTeamException("Invalid match situation");
    }
}