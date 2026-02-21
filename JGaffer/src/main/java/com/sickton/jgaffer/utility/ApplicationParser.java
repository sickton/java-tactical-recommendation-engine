package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.Formation;
import com.sickton.jgaffer.domain.Squad;
import com.sickton.jgaffer.domain.StaminaLevel;
import com.sickton.jgaffer.domain.Style;
import com.sickton.jgaffer.domain.TeamAdaptability;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Utility class for parsing CSV data files into application data structures.
 *
 * <p>Provides static methods to load match titles, squad information,
 * team ID mappings, and team code mappings from league-specific CSV files.</p>
 *
 * @see FileStorage
 * @see com.sickton.jgaffer.domain.Squad
 * @author sickton
 */
public class ApplicationParser {

    /**
     * Parses a match titles CSV file into a map of match IDs to title strings.
     *
     * @param csvPath classpath-relative path to the matches CSV (e.g. "/PremierLeague/PremierLeagueMatches.csv")
     * @return a map where keys are match IDs and values are match title strings
     * @throws RuntimeException if the CSV file cannot be found or read
     */
    public static Map<Integer, String> parseTitles(String csvPath) {
        Map<Integer, String> titles = new HashMap<>();
        try {
            InputStream is = ApplicationParser.class.getResourceAsStream(csvPath);
            if (is == null) throw new RuntimeException(csvPath + " not found on classpath");
            Scanner sc = new Scanner(is);
            int lines = 0;
            while (sc.hasNextLine()) {
                lines++;
                String line = sc.nextLine();
                if (lines <= 1) continue;
                String[] parts = line.split(",");
                titles.put(Integer.parseInt(parts[0]), parts[1]);
            }
            sc.close();
            return titles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses a squad information CSV file into a map of squad names to {@link FileStorage} objects.
     * The CSV must have columns: team_name, code, id, manager, style, team_adaptability, team_stamina, formation
     *
     * @param csvPath classpath-relative path to the squad CSV (e.g. "/PremierLeague/SquadInformation.csv")
     * @return a map where keys are squad names and values are {@link FileStorage} bundles
     * @throws RuntimeException if the CSV file cannot be found or read
     */
    public static Map<String, FileStorage> parseSquadInformation(String csvPath) {
        Map<String, FileStorage> squads = new HashMap<>();
        try {
            InputStream is = ApplicationParser.class.getResourceAsStream(csvPath);
            if (is == null) throw new RuntimeException(csvPath + " not found on classpath");
            Scanner sc = new Scanner(is);
            int lines = 0;
            while (sc.hasNextLine()) {
                lines++;
                String line = sc.nextLine();
                if (lines <= 1) continue;
                String[] parts = line.split(",");
                String squadName = parts[0];
                String manager = parts[3];
                Style teamStyle = Style.valueOf(parts[4]);
                TeamAdaptability adaptability = TeamAdaptability.valueOf(parts[5]);
                StaminaLevel staminaLevel = StaminaLevel.valueOf(parts[6]);
                Formation formation = Formation.valueOf(parts[7].trim());
                FileStorage file = new FileStorage(new Squad(squadName, manager, teamStyle, formation), adaptability, staminaLevel, formation);
                squads.put(squadName, file);
            }
            sc.close();
            return squads;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a map of team IDs to team names by reading the id and team_name columns
     * from the squad information CSV.
     *
     * @param csvPath classpath-relative path to the squad CSV
     * @return a map where keys are integer team IDs and values are team name strings
     * @throws RuntimeException if the CSV file cannot be found or read
     */
    public static Map<Integer, String> buildTeamMapFromCsv(String csvPath) {
        Map<Integer, String> teams = new HashMap<>();
        try {
            InputStream is = ApplicationParser.class.getResourceAsStream(csvPath);
            if (is == null) throw new RuntimeException(csvPath + " not found on classpath");
            Scanner sc = new Scanner(is);
            int lines = 0;
            while (sc.hasNextLine()) {
                lines++;
                String line = sc.nextLine();
                if (lines <= 1) continue;
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[2]);
                String name = parts[0];
                teams.put(id, name);
            }
            sc.close();
            return teams;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the squad information CSV to build a map of team names to their short codes.
     *
     * @param csvPath classpath-relative path to the squad CSV
     * @return a map where keys are full team names and values are abbreviated team codes
     * @throws RuntimeException if the CSV file cannot be found or read
     */
    public static Map<String, String> getTeamCodeMap(String csvPath) {
        Map<String, String> code = new HashMap<>();
        try {
            InputStream is = ApplicationParser.class.getResourceAsStream(csvPath);
            if (is == null) throw new RuntimeException(csvPath + " not found on classpath");
            Scanner sc = new Scanner(is);
            int lines = 0;
            while (sc.hasNextLine()) {
                lines++;
                String line = sc.nextLine();
                if (lines <= 1) continue;
                String[] parts = line.split(",");
                String squadName = parts[0];
                String squadCode = parts[1];
                code.put(squadName, squadCode);
            }
            sc.close();
            return code;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
