package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

/**
 * Tactical rule for the Tension Time phase (minutes 61–70).
 *
 * @see GamePhase#TENSION_TIME
 */
public class TensionTimeTactics extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return checkGamePhase(context.getMinute()) == GamePhase.TENSION_TIME;
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team) {
        throw new UnsupportedOperationException("JGaffer 2.0 — TensionTimeTactics not yet implemented");
    }
}
