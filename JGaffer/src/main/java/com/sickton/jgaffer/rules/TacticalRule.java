package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.exceptions.MatchTeamException;

import java.util.*;

public abstract class TacticalRule {

    protected static final int LATE_MINUTES = 70;
    protected static final int EARLY_MINUTES = 20;
    protected static final double LOW_THRESHOLD = 1.66;
    protected static final double MEDIUM_THRESHOLD = 3.33;
    protected static final double HIGH_THRESHOLD = 5.0;

    public abstract boolean applies(MatchContext context, Team team);

    public abstract Tactic recommend(MatchContext context, Team team);

    protected TeamAdaptability findTeamAdaptability(Team t)
    {
        Map<Player, PlayerState> playingXI = t.getPlayingXI();
        Set<Player> players = playingXI.keySet();
        int totalAdapt = 0;
        int totalPlayers = 0;
        for(Player p : players)
        {
            if(p.getPos() != Position.GK)
            {
                totalAdapt += p.getAdaptabilityScore();
                totalPlayers++;
            }
        }
        if(totalPlayers > 11 || totalPlayers == 0)
            throw new MatchTeamException("Invalid number of players in team");
        double teamAvg = totalAdapt / (double)totalPlayers;
        if(teamAvg <= LOW_THRESHOLD)
            return TeamAdaptability.LOW;
        else if(teamAvg > LOW_THRESHOLD && teamAvg <= MEDIUM_THRESHOLD)
            return TeamAdaptability.MEDIUM;
        else if(teamAvg > MEDIUM_THRESHOLD && teamAvg <= HIGH_THRESHOLD)
            return TeamAdaptability.HIGH;
        return TeamAdaptability.LOW;
    }

    protected boolean isFinalMinutesOfGame(MatchContext context)
    {
        return context.getMinute() >= LATE_MINUTES;
    }

    protected boolean isEarlyMinutesOfGame(MatchContext context)
    {
        return context.getMinute() <= EARLY_MINUTES;
    }

    protected boolean hasHomeAdvantage(MatchContext context, Team team)
    {
        return context.getHome().getName().equals(team.getName());
    }

    protected MatchStatus teamStatus(MatchContext context, Team team) {
        if(context.getHome().getName().equals(team.getName())) {
            if(context.getHomeGoals() > context.getAwayGoals())
                return MatchStatus.WINNING;
            else if(context.getHomeGoals() == context.getAwayGoals())
                return MatchStatus.DRAWING;
            else
                return MatchStatus.LOSING;
        }
        else if(context.getAway().getName().equals(team.getName())) {
            if(context.getAwayGoals() > context.getHomeGoals())
                return MatchStatus.WINNING;
            else if(context.getAwayGoals() == context.getHomeGoals())
                return MatchStatus.DRAWING;
            else
                return MatchStatus.LOSING;
        }
        else
            throw new MatchTeamException("Team entered does not play the match");
    }

    protected int goalDifference(MatchContext context, Team team) {
        if(context.getHome().getName().equals(team.getName())) {
            return context.getHomeGoals() - context.getAwayGoals();
        }
        else if(context.getAway().getName().equals(team.getName())) {
            return context.getAwayGoals() - context.getHomeGoals();
        }
        else
            throw new MatchTeamException("Team entered does not play the match");
    }

    protected boolean redCards(Team team) {
        Map<Player, PlayerState> playing = team.getPlayingXI();
        for(PlayerState p : playing.values()) {
            if(p.isRedCarded())
                return true;
        }
        return false;
    }
}
