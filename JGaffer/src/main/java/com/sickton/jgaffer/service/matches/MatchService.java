package com.sickton.jgaffer.service.matches;

import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.loader.LeagueDataFactory;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Spring service for league and squad data used by the active JGaffer flow.
 *
 * <p>The current product path needs:
 * - club lists for selection
 * - team lookup by name for puzzle graph generation
 *
 * <p>Legacy fixture, match-context, and recommendation logic has been removed.</p>
 */
@Service
public class MatchService {

    public static final String PREMIER_LEAGUE = "PL";
    public static final String SERIE_A = "SA";

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private Map<Integer, String> plTeams;
    private Map<String, FileStorage> plTeamData;

    private Map<Integer, String> saTeams;
    private Map<String, FileStorage> saTeamData;

    @PostConstruct
    public void init() {
        log.info("Loading Premier League squad data...");
        plTeamData = ApplicationParser.parseSquadInformation("/PremierLeague/SquadInformation.csv");
        plTeams = ApplicationParser.buildTeamMapFromCsv("/PremierLeague/SquadInformation.csv");
        log.info("Loaded {} Premier League teams", plTeams.size());

        log.info("Loading Serie A squad data...");
        saTeamData = ApplicationParser.parseSquadInformation("/SerieA/SquadInformation.csv");
        saTeams = ApplicationParser.buildTeamMapFromCsv("/SerieA/SquadInformation.csv");
        log.info("Loaded {} Serie A teams", saTeams.size());
    }

    private Map<Integer, String> teamsFor(String league) {
        return SERIE_A.equals(league) ? saTeams : plTeams;
    }

    private Map<String, FileStorage> teamDataFor(String league) {
        return SERIE_A.equals(league) ? saTeamData : plTeamData;
    }

    public Map<Integer, String> getTeams(String league) {
        return Collections.unmodifiableMap(new TreeMap<>(teamsFor(league)));
    }

    /**
     * Returns the Team object for a given team name in the given league.
     *
     * <p>Tries exact match first, then falls back to case-insensitive prefix/contains
     * so model output like "Newcastle" still matches "Newcastle United".</p>
     */
    public Team getTeam(String league, String teamName) {
        Map<String, FileStorage> teamData = teamDataFor(league);

        if (teamData.containsKey(teamName)) {
            return LeagueDataFactory.buildTeamFromName(teamName, teamData);
        }

        String lower = teamName.toLowerCase();
        for (String key : teamData.keySet()) {
            if (key.toLowerCase().startsWith(lower) || lower.startsWith(key.toLowerCase())) {
                return LeagueDataFactory.buildTeamFromName(key, teamData);
            }
        }

        for (String key : teamData.keySet()) {
            if (key.toLowerCase().contains(lower) || lower.contains(key.toLowerCase())) {
                return LeagueDataFactory.buildTeamFromName(key, teamData);
            }
        }

        return null;
    }
}
