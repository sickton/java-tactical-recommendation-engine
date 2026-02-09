package com.sickton.jgaffer.demoUI;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PremierLeagueFactory {
    protected static final String PREMIER_LEAGUE_MATCH_CONTEXT = "JGaffer/src/main/java/com/sickton/jgaffer/input/MatchMinuteContext.csv";
    protected static Map<Integer, String> titles = ApplicationParser.parseTitles();
    protected static Map<String, FileStorage> teamData = ApplicationParser.parseSquadInformation();

    public static Map<String, MatchContext> buildAllContexts()
    {
        Map<String, MatchContext> map = new HashMap<String, MatchContext>();
        try {
            Scanner sc = new Scanner(new FileInputStream(PREMIER_LEAGUE_MATCH_CONTEXT));
            int lines = 0;
            while(sc.hasNextLine())
            {
                lines++;
                String line = sc.nextLine();
                if(lines <= 1)
                    continue;
                String[] parts = line.split(",");
                int matchId = Integer.parseInt(parts[0]);
                String matchTitle = titles.get(matchId);
                int minute =  Integer.parseInt(parts[1]);
                String homeTeamName =  parts[2];
                FileStorage homeTeamData = teamData.get(homeTeamName);
                TeamAdaptability homeTeamAdaptability = homeTeamData.getAdaptabilityData();
                StaminaLevel homeTeamStaminaLevel = homeTeamData.getStaminaData();
                Squad homeTeamSquad = homeTeamData.getSquadData();
                String awayTeamName = parts[3];
                FileStorage awayTeamData = teamData.get(awayTeamName);
                TeamAdaptability awayTeamAdaptability = awayTeamData.getAdaptabilityData();
                StaminaLevel awayTeamStaminaLevel = awayTeamData.getStaminaData();
                Squad awayTeamSquad = awayTeamData.getSquadData();
                Team homeTeam = new Team(homeTeamSquad, homeTeamStaminaLevel, homeTeamAdaptability);
                Team awayTeam = new Team(awayTeamSquad, awayTeamStaminaLevel, awayTeamAdaptability);
                int homeGoals =  Integer.parseInt(parts[4]);
                int awayGoals =  Integer.parseInt(parts[5]);
                MatchContext context = new MatchContext(matchTitle, homeTeam, awayTeam, homeGoals, awayGoals, minute);
                String key = matchTitle.concat("_".concat(""+minute));
                map.put(key, context);
            }
            sc.close();
            return map;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, String> getFixtureList(String teamCode) {
        Map<Integer, String> matches = new  HashMap<>();
        for(Map.Entry<Integer, String> entry : titles.entrySet())
        {
            if(entry.getValue().contains(teamCode))
                matches.put(entry.getKey(), entry.getValue());
        }
        return matches;
    }

    public static Team buildTeamFromName(String name)
    {
        FileStorage file = teamData.get(name);
        return new Team(file.getSquadData(), file.getStaminaData(), file.getAdaptabilityData());
    }
}
