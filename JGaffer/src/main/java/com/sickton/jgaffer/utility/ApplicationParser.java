package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.Squad;
import com.sickton.jgaffer.domain.StaminaLevel;
import com.sickton.jgaffer.domain.Style;
import com.sickton.jgaffer.domain.TeamAdaptability;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ApplicationParser {
    protected static final String PREMIER_LEAGUE_TITLES = "JGaffer/src/main/java/com/sickton/jgaffer/input/PremierLeagueMatches.csv";
    protected static final String SQUAD_INFORMATION = "JGaffer/src/main/java/com/sickton/jgaffer/input/SquadInformation.csv";

    public static Map<Integer, String> parseTitles()
    {
        Map<Integer, String> titles = new HashMap<Integer, String>();
        try {
            Scanner sc = new Scanner(new FileInputStream(PREMIER_LEAGUE_TITLES));
            int lines = 0;
            while(sc.hasNextLine())
            {
                lines++;
                String line = sc.nextLine();
                if(lines <= 1)
                    continue;
                String[] parts = line.split(",");
                titles.put(Integer.parseInt(parts[0]), parts[1]);
            }
            sc.close();
            return titles;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, FileStorage> parseSquadInformation()
    {
        Map<String, FileStorage> squads = new HashMap<>();
        try
        {
            Scanner sc = new Scanner(new FileInputStream(SQUAD_INFORMATION));
            int lines = 0;
            while(sc.hasNextLine())
            {
                lines++;
                String line = sc.nextLine();
                if(lines <= 1)
                    continue;
                String[] parts = line.split(",");
                String squadName = parts[0];
                String squadCode = parts[1];
                String id = parts[2];
                String manager = parts[3];
                Style teamStyle = Style.valueOf(parts[4]);
                TeamAdaptability adaptability = TeamAdaptability.valueOf(parts[5]);
                StaminaLevel  staminaLevel = StaminaLevel.valueOf(parts[6]);
                FileStorage file = new FileStorage(new Squad(squadName, manager, teamStyle), adaptability, staminaLevel);
                squads.put(squadName, file);
            }
            sc.close();
            return squads;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, String> buildTeamMap()
    {
        Map<Integer, String> teams = new HashMap<>();
        teams.put(1, "Arsenal");
        teams.put(2, "Aston Villa");
        teams.put(3, "Bournemouth");
        teams.put(4, "Brentford");
        teams.put(5, "Brighton");
        teams.put(6, "Chelsea");
        teams.put(7, "Crystal Palace");
        teams.put(8, "Everton");
        teams.put(9, "Fulham");
        teams.put(10, "Liverpool");
        teams.put(11, "Manchester City");
        teams.put(12, "Manchester United");
        teams.put(13, "Newcastle United");
        teams.put(14, "Nottingham Forest");
        teams.put(15, "Tottenham Hotspur");
        teams.put(16, "West Ham United");
        teams.put(17, "Wolves");
        teams.put(18, "Leicester City");
        teams.put(19, "Ipswich Town");
        teams.put(20, "Southampton");
        return teams;
    }

    public static Map<String, String> getTeamCodeMap()
    {
        Map<String, String> code = new HashMap<>();
        try
        {
            Scanner sc = new Scanner(new FileInputStream(SQUAD_INFORMATION));
            int lines = 0;
            while(sc.hasNextLine())
            {
                lines++;
                String line = sc.nextLine();
                if(lines <= 1)
                    continue;
                String[] parts = line.split(",");
                String squadName = parts[0];
                String squadCode = parts[1];
                code.put(squadName, squadCode);
            }
            sc.close();
            return code;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
