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

/**
 * Utility class for parsing CSV data files into application data structures.
 *
 * <p>Provides static methods to load Premier League match titles, squad information,
 * team ID mappings, and team code mappings from CSV files. Acts as the primary
 * data access layer between raw CSV input and the domain model.</p>
 *
 * @see FileStorage
 * @see com.sickton.jgaffer.domain.Squad
 * @author sickton
 */
public class ApplicationParser {
    protected static final String PREMIER_LEAGUE_TITLES = "JGaffer/src/main/java/com/sickton/jgaffer/input/PremierLeagueMatches.csv";
    protected static final String SQUAD_INFORMATION = "JGaffer/src/main/java/com/sickton/jgaffer/input/SquadInformation.csv";

    /**
     * Parses the Premier League match titles CSV file into a map of match IDs to title strings.
     *
     * @return a map where keys are match IDs and values are match title strings
     * @throws RuntimeException if the CSV file cannot be found or read
     */
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

    /**
     * Parses the squad information CSV file into a map of squad names to {@link FileStorage} objects.
     *
     * @return a map where keys are squad names and values are {@link FileStorage} bundles
     *         containing squad, adaptability, and stamina data
     * @throws RuntimeException if the CSV file cannot be found or read
     */
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

    /**
     * Builds a static map of team IDs to Premier League team names for the current season.
     *
     * @return a map where keys are integer team IDs (1-20) and values are team name strings
     */
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

    /**
     * Parses the squad information CSV to build a map of team names to their short codes.
     *
     * @return a map where keys are full team names and values are abbreviated team codes
     * @throws RuntimeException if the CSV file cannot be found or read
     */
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
