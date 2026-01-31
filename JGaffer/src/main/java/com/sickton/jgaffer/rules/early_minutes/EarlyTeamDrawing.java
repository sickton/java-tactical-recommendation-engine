package com.sickton.jgaffer.rules.early_minutes;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.MatchStatus;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.rules.TacticalRule;

public class EarlyTeamDrawing extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return !redCards(team) && isEarlyMinutesOfGame(context) && (teamStatus(context, team) == MatchStatus.DRAWING);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {

        boolean home = hasHomeAdvantage(context, team);

        double possession = home
                ? context.getHomePossession()
                : context.getAwayPossession();

        if (possession >= 60.0) {
            return Tactic.POSSESSION;
        }

        return Tactic.MID_BLOCK;
    }

}
