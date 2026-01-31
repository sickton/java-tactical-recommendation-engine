package com.sickton.jgaffer.rules.early_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class EarlyTeamLosing extends TacticalRule {
    @Override
    public boolean applies(MatchContext context, Team team) {
        return isEarlyMinutesOfGame(context) && (teamStatus(context, team) == MatchStatus.LOSING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {

        int gd = goalDifference(context, team);
        TeamAdaptability adaptability = findTeamAdaptability(team);

        if (gd <= -2 && adaptability == TeamAdaptability.HIGH) {
            return Tactic.MID_BLOCK;
        }

        return Tactic.MID_BLOCK;
    }
}
