package com.sickton.jgaffer.rules.late_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class LateTeamWinning extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && isFinalMinutesOfGame(context) && (teamStatus(context, team) == MatchStatus.WINNING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {

        boolean home = hasHomeAdvantage(context, team);
        TeamAdaptability adaptability = findTeamAdaptability(team);
        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();
        // Comfortable control of the game
        if (possession >= 55.0) {
            return adaptability == TeamAdaptability.LOW
                    ? Tactic.MID_BLOCK
                    : Tactic.POSSESSION;
        }
        // Struggling to keep the ball
        if (possession < 50.0) {
            return adaptability == TeamAdaptability.LOW
                    ? Tactic.LOW_BLOCK
                    : Tactic.MID_BLOCK;
        }
        // Neutral scenario
        return home && adaptability != TeamAdaptability.LOW
                ? Tactic.POSSESSION
                : Tactic.MID_BLOCK;
    }
}
