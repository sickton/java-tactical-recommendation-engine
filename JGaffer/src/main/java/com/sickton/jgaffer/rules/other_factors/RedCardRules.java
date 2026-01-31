package com.sickton.jgaffer.rules.other_factors;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.rules.TacticalRule;

public class RedCardRules extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return redCards(team);
    }

    @Override
    public Tactic recommend(MatchContext context, Team team) {
        boolean isEarly = isEarlyMinutesOfGame(context);
        boolean isLate = isFinalMinutesOfGame(context);
        boolean isMiddle = !isEarly && !isLate;
        if(isEarly)
        {
            return Tactic.MID_BLOCK;
        }
        else if(isLate)
        {
            return Tactic.LOW_BLOCK;
        }
        else
        {
            return Tactic.POSSESSION;
        }
    }
}
