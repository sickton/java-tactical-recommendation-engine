package com.sickton.jgaffer.rules.game_phases;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.TacticalRule;

/**
 * Tactical rule for the Late Game phase (minutes 71–87).
 *
 * @see GamePhase#LATE_GAME
 */
public class LateGameTactics extends TacticalRule {

    @Override
    public boolean applies(MatchContext context, Team team) {
        return checkGamePhase(context.getMinute()) == GamePhase.LATE_GAME;
    }

    @Override
    public TacticRecommendation recommendWithConfidence(MatchContext context, Team team) {
        throw new UnsupportedOperationException("JGaffer 2.0 — LateGameTactics not yet implemented");
    }
}
