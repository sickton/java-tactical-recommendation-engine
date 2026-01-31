package com.sickton.jgaffer.rules.middle_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class MiddleTeamWinning extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && !isFinalMinutesOfGame(context) && !isEarlyMinutesOfGame(context)
                && (teamStatus(context, team) == MatchStatus.WINNING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {
        int gd = goalDifference(context, team);
        TeamAdaptability adaptability = findTeamAdaptability(team);
        boolean home = hasHomeAdvantage(context, team);

        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();
        if (gd >= 2) {
            return possession >= 55.0
                    ? Tactic.POSSESSION
                    : Tactic.MID_BLOCK;
        }
        // Narrow lead
        if (possession >= 55.0 && adaptability == TeamAdaptability.HIGH) {
            return Tactic.POSSESSION;
        }
        return Tactic.MID_BLOCK;
    }
}
