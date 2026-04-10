package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

/**
 * Tactical rule for the Closing Half phase (minutes 16–44).
 *
 * @see GamePhase#CLOSING_HALF
 */
public class ClosingHalfTactics extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return checkGamePhase(context.getMinute()) == GamePhase.CLOSING_HALF;
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team) {
        throw new UnsupportedOperationException("JGaffer 2.0 — ClosingHalfTactics not yet implemented");
    }
}
