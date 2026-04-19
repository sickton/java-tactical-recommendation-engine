package com.sickton.jgaffer.loader;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Factory class for constructing league domain objects from CSV data.
 *
 * <p>Builds the complete set of {@link MatchContext} objects from a
 * MatchMinuteContext CSV file, keyed by "{@code matchTitle_minute}" for quick lookup.
 * Also provides methods to retrieve fixture lists for a specific team and to
 * construct {@link Team} objects from team names.</p>
 *
 * @see MatchContext
 * @see Team
 * @see ApplicationParser
 * @author sickton
 */
public class LeagueDataFactory {

    /**
     * Builds all MatchContext objects from the given match-minute context CSV.
     *
     * @param contextCsvPath classpath path to MatchMinuteContext.csv
     * @param titles         map of matchId → matchTitle (e.g. "ARS-AVL")
     * @param teamData       map of teamName → FileStorage
     * @return map keyed by "matchTitle_minute" → MatchContext
     */
    public static Map<String, MatchContext> buildAllContexts(
            String contextCsvPath,
            Map<Integer, String> titles,
            Map<String, FileStorage> teamData) {

        Map<String, MatchContext> map = new HashMap<>();
        try {
            InputStream is = LeagueDataFactory.class.getResourceAsStream(contextCsvPath);
            if (is == null) throw new RuntimeException(contextCsvPath + " not found on classpath");
            Scanner sc = new Scanner(is);
            int lines = 0;
            while (sc.hasNextLine()) {
                lines++;
                String line = sc.nextLine();
                if (lines <= 1) continue;
                String[] parts = line.split(",");
                int matchId = Integer.parseInt(parts[0]);
                String matchTitle = titles.get(matchId);
                int minute = Integer.parseInt(parts[1]);
                String homeTeamName = parts[2];
                FileStorage homeTeamData = teamData.get(homeTeamName);
                TeamAdaptability homeTeamAdaptability = homeTeamData.getAdaptabilityData();
                StaminaLevel homeTeamStaminaLevel = homeTeamData.getStaminaData();
                Squad homeTeamSquad = homeTeamData.getSquadData();
                String awayTeamName = parts[3];
                FileStorage awayTeamData = teamData.get(awayTeamName);
                TeamAdaptability awayTeamAdaptability = awayTeamData.getAdaptabilityData();
                StaminaLevel awayTeamStaminaLevel = awayTeamData.getStaminaData();
                Squad awayTeamSquad = awayTeamData.getSquadData();
                Team homeTeam = homeTeamData.hasCustomWeights()
                        ? new Team(homeTeamSquad, homeTeamStaminaLevel, homeTeamAdaptability,
                                   homeTeamData.getAtkWeight(), homeTeamData.getDefWeight(), homeTeamData.getCtrlWeight())
                        : new Team(homeTeamSquad, homeTeamStaminaLevel, homeTeamAdaptability);
                Team awayTeam = awayTeamData.hasCustomWeights()
                        ? new Team(awayTeamSquad, awayTeamStaminaLevel, awayTeamAdaptability,
                                   awayTeamData.getAtkWeight(), awayTeamData.getDefWeight(), awayTeamData.getCtrlWeight())
                        : new Team(awayTeamSquad, awayTeamStaminaLevel, awayTeamAdaptability);
                int homeGoals = Integer.parseInt(parts[4]);
                int awayGoals = Integer.parseInt(parts[5]);
                MatchContext context = new MatchContext(matchTitle, homeTeam, awayTeam, homeGoals, awayGoals, minute);
                String key = matchTitle + "_" + minute;
                map.put(key, context);
            }
            sc.close();
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns all fixture IDs and titles that include the given team code. */
    public static Map<Integer, String> getFixtureList(String teamCode, Map<Integer, String> titles) {
        Map<Integer, String> matches = new HashMap<>();
        for (Map.Entry<Integer, String> entry : titles.entrySet()) {
            if (entry.getValue().contains(teamCode))
                matches.put(entry.getKey(), entry.getValue());
        }
        return matches;
    }

    /** Builds a Team domain object from the given team name and data map.
     *  Uses PCA-derived weights when present; falls back to style-bias otherwise.
     *  Returns null if the team name is not found in the data map. */
    public static Team buildTeamFromName(String name, Map<String, FileStorage> teamData) {
        FileStorage file = teamData.get(name);
        if (file == null) return null;
        return file.hasCustomWeights()
                ? new Team(file.getSquadData(), file.getStaminaData(), file.getAdaptabilityData(),
                           file.getAtkWeight(), file.getDefWeight(), file.getCtrlWeight())
                : new Team(file.getSquadData(), file.getStaminaData(), file.getAdaptabilityData());
    }
}
