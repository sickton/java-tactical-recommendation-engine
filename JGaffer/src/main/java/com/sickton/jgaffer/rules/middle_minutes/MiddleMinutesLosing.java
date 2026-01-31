package com.sickton.jgaffer.rules.middle_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class MiddleMinutesLosing extends TacticalRule {
    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && !isFinalMinutesOfGame(context) && !isEarlyMinutesOfGame(context)
                && (teamStatus(context, team) == MatchStatus.LOSING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {
        int gd = goalDifference(context, team);
        TeamAdaptability adaptability = findTeamAdaptability(team);
        boolean home = hasHomeAdvantage(context, team);
        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();
        if (gd <= -2) {
            return adaptability == TeamAdaptability.HIGH
                    ? Tactic.HIGH_PRESS
                    : Tactic.MID_BLOCK;
        }
        // Narrow deficit
        if (possession >= 55.0 && adaptability != TeamAdaptability.LOW) {
            return Tactic.HIGH_PRESS;
        }
        return Tactic.MID_BLOCK;
    }
}
