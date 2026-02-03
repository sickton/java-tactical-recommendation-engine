package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;
import com.sickton.jgaffer.utility.TacticMapper;

public class EarlyMinuteTactics extends TacticalRule {
    @Override
    public boolean applies(MatchContext context, Team team) {
        GamePhase phase = super.checkGamePhase(context.getMinute());
        return phase == GamePhase.EARLY_MINUTES;
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {
        int goalDiff = getGoalDifference(context);
        TeamAdaptability adapt = getTeamAdaptability(team);
        if(isTeamWinning(context,team)){

        }
        else if(isTeamDrawing(context,team)){

        }
        else if(isTeamLosing(context,team)){

        }
        else
            throw new IllegalArgumentException("Invalid match situation provided");
    }
}
