package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

/**
 * Tactical rule for the Stoppage Time phase (minutes 88+).
 *
 * @see GamePhase#STOPPAGE_TIME
 */
public class StoppageTimeTactics extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return checkGamePhase(context.getMinute()) == GamePhase.STOPPAGE_TIME;
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team) {
        throw new UnsupportedOperationException("JGaffer 2.0 — StoppageTimeTactics not yet implemented");
    }
}
