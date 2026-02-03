package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;

public abstract class TacticalRule {
     /* Thresholds for time phases in a football game.
     0-15 early minutes
     16-44 closing half
     45-50 half-time
     51-60 building phase
     61-70 tension time
     71-87 late game
     88+ stoppage time */
    protected static final int EARLY_MINUTE_THRESHOLD = 15;
    protected static final int FIRST_HALF_THRESHOLD = 45;
    protected static final int SECOND_HALF_THRESHOLD = 50;
    protected static final int ASSESSING_TIME = 60;
    protected static final int TENSION_TIME_THRESHOLD = 70;
    protected static final int STOPPAGE_TIME = 88;

    /* thresholds for team adaptability ranges */
    protected static final double LOW_THRESHOLD = 1.66;
    protected static final double MEDIUM_THRESHOLD = 3.33;
    protected static final double HIGH_THRESHOLD = 5.0;

    protected static final double LOW_INTENT_THRESHOLD = 0.33;
    protected static final double MEDIUM_INTENT_THRESHOLD = 0.66;
    protected static final double HIGH_INTENT_THRESHOLD = 1;

    public abstract boolean applies(MatchContext context, Team team);

    public abstract Tactic recommend(MatchContext context, Team team);

    public WeightCombination adjustWeights(double attack, double control, double defence) {
        IntentRange attackIntent = getIntent(attack);
        IntentRange controlIntent = getIntent(control);
        IntentRange defenceIntent = getIntent(defence);

        return new WeightCombination(attackIntent, defenceIntent, controlIntent);
    }

    public IntentRange getIntent(double intent) {
        if(intent >= 0.0 && intent <= LOW_INTENT_THRESHOLD)
            return IntentRange.LOW;
        else if(intent > LOW_INTENT_THRESHOLD && intent <= MEDIUM_INTENT_THRESHOLD)
            return IntentRange.MEDIUM;
        else if(intent > MEDIUM_INTENT_THRESHOLD && intent <= HIGH_INTENT_THRESHOLD)
            return IntentRange.HIGH;
        else
            throw new IllegalArgumentException("Invalid intent level");
    }
}