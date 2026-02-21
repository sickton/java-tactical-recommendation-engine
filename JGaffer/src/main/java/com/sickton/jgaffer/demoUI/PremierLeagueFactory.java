package com.sickton.jgaffer.demoUI;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;

import java.util.Map;

/**
 * Premier League-specific convenience wrapper around {@link LeagueDataFactory}.
 *
 * <p>Retains the original API for the CLI ({@code jgafferApplication}) while
 * delegating all data loading to the generic factory with PL-specific paths.</p>
 *
 * @author sickton
 */
public class PremierLeagueFactory {

    private static final String PL_TITLES_PATH   = "/PremierLeague/PremierLeagueMatches.csv";
    private static final String PL_SQUAD_PATH    = "/PremierLeague/SquadInformation.csv";
    private static final String PL_CONTEXT_PATH  = "/PremierLeague/MatchMinuteContext.csv";

    protected static final Map<Integer, String>    titles   = ApplicationParser.parseTitles(PL_TITLES_PATH);
    protected static final Map<String, FileStorage> teamData = ApplicationParser.parseSquadInformation(PL_SQUAD_PATH);

    public static Map<String, MatchContext> buildAllContexts() {
        return LeagueDataFactory.buildAllContexts(PL_CONTEXT_PATH, titles, teamData);
    }

    public static Map<Integer, String> getFixtureList(String teamCode) {
        return LeagueDataFactory.getFixtureList(teamCode, titles);
    }

    public static Team buildTeamFromName(String name) {
        return LeagueDataFactory.buildTeamFromName(name, teamData);
    }
}
