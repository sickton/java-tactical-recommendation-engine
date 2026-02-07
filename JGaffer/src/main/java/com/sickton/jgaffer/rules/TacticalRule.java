package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;

import java.util.Map;

public abstract class TacticalRule {
     /** Thresholds for time phases in a football game.
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

    protected static final double LOW_INTENT_THRESHOLD = 0.33;
    protected static final double MEDIUM_INTENT_THRESHOLD = 0.66;
    protected static final double HIGH_INTENT_THRESHOLD = 1;

    public abstract boolean applies(MatchContext context, Team team);

    public abstract Tactic recommend(MatchContext context, Team team, Map<TacticKey, TacticSuggestion> tacticMap);

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

    public GamePhase checkGamePhase(int minute) {
        if(minute >= 0 && minute <= EARLY_MINUTE_THRESHOLD)
            return GamePhase.EARLY_MINUTES;
        else if(minute > EARLY_MINUTE_THRESHOLD && minute < FIRST_HALF_THRESHOLD)
            return GamePhase.CLOSING_HALF;
        else if(minute >= FIRST_HALF_THRESHOLD && minute <= SECOND_HALF_THRESHOLD)
            return GamePhase.HALF_TIME;
        else if(minute > SECOND_HALF_THRESHOLD && minute <= ASSESSING_TIME)
            return GamePhase.BUILD_PHASE;
        else if(minute > ASSESSING_TIME && minute <= TENSION_TIME_THRESHOLD)
            return GamePhase.TENSION_TIME;
        else if(minute > TENSION_TIME_THRESHOLD && minute < STOPPAGE_TIME)
            return GamePhase.LATE_GAME;
        else if(minute >= STOPPAGE_TIME)
            return GamePhase.STOPPAGE_TIME;
        throw new IllegalArgumentException("Invalid minute");
    }

    public boolean isTeamWinning(MatchContext context, Team t) {
        if(t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() > context.getAwayGoals();
        else if(t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() > context.getHomeGoals();
        else
            throw new IllegalArgumentException("Invalid Team input. Team not playing this match");
    }

    public boolean isTeamLosing(MatchContext context, Team t) {
        if(t.getName().equalsIgnoreCase(context.getHome().getName()))
            return context.getHomeGoals() < context.getAwayGoals();
        else if(t.getName().equalsIgnoreCase(context.getAway().getName()))
            return context.getAwayGoals() < context.getHomeGoals();
        else
            throw new IllegalArgumentException("Invalid Team input. Team not playing this match");
    }

    public boolean isTeamDrawing(MatchContext context, Team t) {
        if(!t.getName().equalsIgnoreCase(context.getHome().getName()) &&  !t.getName().equalsIgnoreCase(context.getAway().getName()))
            throw new IllegalArgumentException("Invalid team. Not playing this match");
        return context.getHomeGoals() == context.getAwayGoals();
    }

    public int getGoalDifference(MatchContext context) {
        return Math.abs(context.getHomeGoals() - context.getAwayGoals());
    }

    public StaminaLevel getTeamStamina(Team team) {
        return team.getStaminaLevel();
    }

    public TeamAdaptability getTeamAdaptability(Team team) {
        return team.getAdaptabilityLevel();
    }

    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}