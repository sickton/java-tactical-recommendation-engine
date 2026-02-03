package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;

import java.util.HashMap;
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

    /** thresholds for team adaptability ranges */
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

    /** Method would return the number of players on the pitch for the team. If any less than 11, the
     * system assumes red cards */
    public int playersOnPitch(Team t) {
        return t.getPlayingXI().size();
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
        Map<Player, PlayerState> players = team.getPlayingXI();
        if(players.isEmpty())
            throw new IllegalArgumentException("Team has no playing XI");
        Map<StaminaLevel, Integer> staminas = new HashMap<>();
        staminas.put(StaminaLevel.HIGH, 3);
        staminas.put(StaminaLevel.MEDIUM, 2);
        staminas.put(StaminaLevel.LOW, 1);
        int staminaLevel = 0;
        for(Player player: players.keySet()) {
            PlayerState playerState = players.get(player);
            staminaLevel += staminas.get(playerState.getStamina());
        }
        int teamStamina = staminaLevel / players.size();
        if(teamStamina == 1)
            return StaminaLevel.LOW;
        else if(teamStamina == 2)
            return StaminaLevel.MEDIUM;
        else if(teamStamina == 3)
            return StaminaLevel.HIGH;
        throw new IllegalArgumentException("Invalid Team input");
    }

    public TeamAdaptability getTeamAdaptability(Team team) {
        Map<Player, PlayerState> players = team.getPlayingXI();
        if(players.isEmpty())
            throw new IllegalArgumentException("Team has no playing XI");
        double teamAdaptabilityScore = 0;
        for(Player player: players.keySet()) {
            teamAdaptabilityScore += player.getAdaptabilityScore();
        }
        double teamAdaptability =  teamAdaptabilityScore / players.size();
        if(teamAdaptability >= 0 && teamAdaptability <= LOW_THRESHOLD)
            return TeamAdaptability.LOW;
        else if(teamAdaptability > LOW_THRESHOLD && teamAdaptability <= MEDIUM_THRESHOLD)
            return TeamAdaptability.MEDIUM;
        else if(teamAdaptability > MEDIUM_THRESHOLD && teamAdaptability <= HIGH_THRESHOLD)
            return TeamAdaptability.HIGH;
        else
            throw new IllegalArgumentException("Invalid Team input");
    }
}