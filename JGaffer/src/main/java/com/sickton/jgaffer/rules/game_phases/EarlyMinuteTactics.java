package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

/**
 * Tactical rule for the Early Minutes phase (minutes 0–15).
 *
 * @see GamePhase#EARLY_MINUTES
 */
public class EarlyMinuteTactics extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return checkGamePhase(context.getMinute()) == GamePhase.EARLY_MINUTES;
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team) {
        throw new UnsupportedOperationException("JGaffer 2.0 — EarlyMinuteTactics not yet implemented");
    }
}
