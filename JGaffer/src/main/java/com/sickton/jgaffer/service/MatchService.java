package com.sickton.jgaffer.service;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.demoUI.PremierLeagueFactory;
import com.sickton.jgaffer.engine.TacticalRecommendationEngine;
import com.sickton.jgaffer.openAIService.OpenAIClient;
import com.sickton.jgaffer.openAIService.TacticalExplanationService;
import com.sickton.jgaffer.utility.ApplicationParser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spring service that encapsulates match data loading and recommendation logic.
 *
 * <p>Extracts the orchestration code from the CLI {@code jgafferApplication} into
 * a reusable service bean. Holds all match data in memory after startup to avoid
 * repeated CSV parsing on each request.</p>
 *
 * @author sickton
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final TacticalRecommendationEngine engine = new TacticalRecommendationEngine();
    private final Map<Integer, String> teams = ApplicationParser.buildTeamMap();
    private final Map<Integer, String> titles = ApplicationParser.parseTitles();
    private final Map<String, String> teamCodes = ApplicationParser.getTeamCodeMap();

    private Map<String, MatchContext> matchContextMap;

    @PostConstruct
    public void init() {
        log.info("Loading match context data...");
        matchContextMap = PremierLeagueFactory.buildAllContexts();
        log.info("Loaded {} match contexts", matchContextMap.size());
    }

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    /** All 20 Premier League teams keyed by their display ID (1-20). */
    public Map<Integer, String> getTeams() {
        return Collections.unmodifiableMap(teams);
    }

    /**
     * Returns fixtures for the given team, split into home and away lists.
     * Each inner map has keys "id" (Integer matchId) and "title" (String match code).
     */
    public Map<String, List<Map<String, Object>>> getFixtures(String teamName) {
        String code = teamCodes.get(teamName);
        Map<Integer, String> all = new TreeMap<>(PremierLeagueFactory.getFixtureList(code));

        List<Map<String, Object>> home = new ArrayList<>();
        List<Map<String, Object>> away = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : all.entrySet()) {
            Map<String, Object> fixture = new LinkedHashMap<>();
            fixture.put("id", entry.getKey());
            fixture.put("title", entry.getValue());
            if (entry.getValue().startsWith(code)) {
                home.add(fixture);
            } else {
                away.add(fixture);
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("home", home);
        result.put("away", away);
        return result;
    }

    /**
     * Fetches the pre-built MatchContext for a given match number and minute.
     * Returns null if the combination is not found (match/minute not in CSV data).
     */
    public MatchContext getMatchContext(int matchNumber, int minute) {
        String matchTitle = titles.get(matchNumber);
        if (matchTitle == null) return null;
        return matchContextMap.get(matchTitle + "_" + minute);
    }

    /** Returns the team object for a given team name. */
    public Team getTeam(String teamName) {
        return PremierLeagueFactory.buildTeamFromName(teamName);
    }

    /** Returns the team name for a given team ID. */
    public String getTeamName(int teamId) {
        return teams.get(teamId);
    }

    /** Returns the short code (e.g. "MCI") for a given team name. */
    public String getTeamCode(String teamName) {
        return teamCodes.get(teamName);
    }

    /** Runs the engine and returns the full recommendation (tactic + formation + confidence). */
    public TacticRecommendation getRecommendation(MatchContext context, Team team) {
        return engine.recommendWithDetails(context, team);
    }

    /**
     * Builds the AI explanation prompt and calls the explanation service.
     * Returns a fallback string if the API is unavailable.
     */
    public String getExplanation(MatchContext context, Team team, TacticRecommendation recommendation) {
        Team opponent = context.getHome().getName().equalsIgnoreCase(team.getName())
                ? context.getAway() : context.getHome();

        int teamGoals = context.getHome().getName().equalsIgnoreCase(team.getName())
                ? context.getHomeGoals() : context.getAwayGoals();
        int opponentGoals = context.getHome().getName().equalsIgnoreCase(team.getName())
                ? context.getAwayGoals() : context.getHomeGoals();

        int goalDiff = teamGoals - opponentGoals;
        String scoreline = team.getName() + " " + teamGoals + " - " + opponent.getName() + " " + opponentGoals;

        int minute = context.getMinute();
        String gamePhase;
        if      (minute <= 15)  gamePhase = "Early Minutes (0-15)";
        else if (minute <= 44)  gamePhase = "Closing Half (16-44)";
        else if (minute <= 50)  gamePhase = "Half Time (45-50)";
        else if (minute <= 60)  gamePhase = "Build Phase (51-60)";
        else if (minute <= 70)  gamePhase = "Tension Time (61-70)";
        else if (minute <= 87)  gamePhase = "Late Game (71-87)";
        else                    gamePhase = "Stoppage Time (88+)";

        String prompt = """
You are the head coach of %s delivering a clear tactical briefing to your players.

Context:
Recommended tactic: %s
Suggested formation: %s
Opponent: %s
Team style: %s | Opponent style: %s
Stamina: %s | Adaptability: %s
Game phase: %s
Minute: %d
Score: %s
Goal difference from your perspective: %d
Engine confidence: %d%%

Assume %s is the team being managed. Interpret the score and goal difference from this team's perspective (winning, losing, or drawing).

Use plain ASCII only. No markdown, no bold text, no emojis, no smart quotes.

Give exactly 5 bullet points using a dash (-):
- Points 1-2: explain why this tactic fits the current scoreline and game phase.
- Point 3: explain the tactical matchup (style vs style and formation structure).
- Point 4: explain the main risk this tactic accepts or prevents.
- Point 5: a short, direct, imperative instruction to the team (motivational but realistic).
""".formatted(
                team.getName(),
                recommendation.getTactic(),
                recommendation.getSuggestedFormation().getLabel(),
                opponent.getName(),
                team.getSquad().getTeamStyle().name(),
                opponent.getSquad().getTeamStyle().name(),
                team.getStaminaLevel().name(),
                team.getAdaptabilityLevel().name(),
                gamePhase,
                minute,
                scoreline,
                goalDiff,
                recommendation.getConfidence(),
                team.getName()
        );

        OpenAIClient client = new OpenAIClient(openAiApiKey);
        TacticalExplanationService service = new TacticalExplanationService(client);
        return service.generateExplanation(prompt);
    }

    /** Returns all available tactics as a list for rendering in the UI. */
    public List<Tactic> getAllTactics() {
        return List.of(Tactic.values());
    }
}
