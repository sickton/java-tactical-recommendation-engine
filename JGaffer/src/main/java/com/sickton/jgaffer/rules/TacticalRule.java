package com.sickton.jgaffer.rules;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.exceptions.MatchTeamException;

import java.util.*;

public abstract class TacticalRule {

    public abstract boolean applies(MatchContext context, Team team);

    public abstract Tactic recommend(MatchContext context, Team team);

    protected TeamAdaptability findTeamAdaptability(Team t)
    {
        Map<Player, PlayerState> playingXI = t.getPlayingXI();
        Set<Player> players = playingXI.keySet();
        int totalAdapt = 0;
        for(Player p : players)
        {
            if(p.getPos() == Position.GK)
                continue;
            else
                totalAdapt += p.getAdaptabilityScore();
        }
        double teamAvg = totalAdapt / 10.0;
        if(teamAvg <= 1.66)
            return TeamAdaptability.LOW;
        else if(teamAvg > 1.66 && teamAvg <= 3.33)
            return TeamAdaptability.MEDIUM;
        else if(teamAvg > 3.33 && teamAvg <= 5.0)
            return TeamAdaptability.HIGH;
        return TeamAdaptability.LOW;
    }

    protected boolean isFinalMinutesOfGame(MatchContext context)
    {
        return context.getMinute() >= 70;
    }

    protected boolean isEarlyMinutesOfGame(MatchContext context)
    {
        return context.getMinute() <= 20;
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
}
