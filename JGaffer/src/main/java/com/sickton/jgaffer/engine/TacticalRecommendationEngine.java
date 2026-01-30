package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.exceptions.*;

public class TacticalRecommendationEngine {
    public Tactic recommend(MatchContext context, Team team)
    {
        boolean currentTeamWin = false;
        if(context.getHome().getName().equals(team.getName()))
            currentTeamWin = context.getHomeGoals() > context.getAwayGoals();
        else if(context.getAway().getName().equals(team.getName()))
            currentTeamWin = context.getAwayGoals() > context.getHomeGoals();
        if(context.getMinute() >= 70)
        {
            if(currentTeamWin)
                return Tactic.POSSESSION;
            else
                return Tactic.HIGH_PRESS;
        }
        return Tactic.MID_BLOCK;
    }

    /*public TeamAdaptibility findTeamAdaptability(Team t)
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
        double teamAvg = totalAdapt / 10;
        if(teamAvg <= 1.66)
            return TeamAdaptability.LOW;
        else if(teamAvg > 1.66 && teamAvg <= 3.33)
            return TeamAdaptability.MEDIUM;
        else if(teamAvg > 3.33 && teamAvg <= 5.0)
            return TeamAdaptability.HIGH;
    }*/
}