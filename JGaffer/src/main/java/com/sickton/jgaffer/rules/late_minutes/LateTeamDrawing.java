package com.sickton.jgaffer.rules.late_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class LateTeamDrawing extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && isFinalMinutesOfGame(context) && (teamStatus(context, team) == MatchStatus.DRAWING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {

        TeamAdaptability adaptability = findTeamAdaptability(team);
        boolean home = hasHomeAdvantage(context, team);

        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();
        // Controlling the game
        if (possession >= 55.0) {
            if (home && adaptability != TeamAdaptability.LOW) {
                return Tactic.HIGH_PRESS;
            }
            return Tactic.POSSESSION;
        }
        // Lacking control
        if (possession < 50.0) {
            if (!home && adaptability == TeamAdaptability.HIGH) {
                return Tactic.COUNTER;
            }
            return adaptability == TeamAdaptability.LOW
                    ? Tactic.LOW_BLOCK
                    : Tactic.MID_BLOCK;
        }
        // Neutral late-game draw
        return Tactic.MID_BLOCK;
    }
}
