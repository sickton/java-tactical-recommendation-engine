package com.sickton.jgaffer.rules.late_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class LateTeamLosing extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && isFinalMinutesOfGame(context)
                && teamStatus(context, team) == MatchStatus.LOSING;
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {

        TeamAdaptability adaptability = findTeamAdaptability(team);
        boolean home = hasHomeAdvantage(context, team);

        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();

        // Already pushing but not breaking through → press harder
        if (possession >= 55.0) {
            return adaptability == TeamAdaptability.LOW
                    ? Tactic.MID_BLOCK
                    : Tactic.HIGH_PRESS;
        }

        // Little control → force transitions
        if (possession < 50.0) {
            if (adaptability == TeamAdaptability.LOW) {
                return Tactic.MID_BLOCK;
            }
            return home
                    ? Tactic.HIGH_PRESS
                    : Tactic.COUNTER;
        }

        // Neutral losing scenario
        return adaptability == TeamAdaptability.HIGH
                ? Tactic.HIGH_PRESS
                : Tactic.MID_BLOCK;
    }
}
