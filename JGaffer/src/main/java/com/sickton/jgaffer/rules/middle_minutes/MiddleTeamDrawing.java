package com.sickton.jgaffer.rules.middle_minutes;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

public class MiddleTeamDrawing extends TacticalRule {
    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && !isFinalMinutesOfGame(context) && !isEarlyMinutesOfGame(context)
                && (teamStatus(context, team) == MatchStatus.DRAWING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {
        TeamAdaptability adaptability = findTeamAdaptability(team);
        boolean home = hasHomeAdvantage(context, team);
        double possession = home
                ? context.getHomePossession() : context.getAwayPossession();
        // Controlling the game → establish rhythm
        if (possession >= 55.0) {
            return Tactic.POSSESSION;
        }
        // Lacking control → stabilize structure
        if (possession < 45.0) {
            return adaptability == TeamAdaptability.LOW
                    ? Tactic.MID_BLOCK : Tactic.POSSESSION;
        }
        // Neutral draw → manage space and tempo
        return Tactic.MID_BLOCK;
    }
}
