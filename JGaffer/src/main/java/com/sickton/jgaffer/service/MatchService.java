package com.sickton.jgaffer.service;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.demoUI.LeagueDataFactory;
import com.sickton.jgaffer.engine.TacticalRecommendationEngine;
import com.sickton.jgaffer.openAIService.OpenAIClient;
import com.sickton.jgaffer.openAIService.TacticalExplanationService;
import com.sickton.jgaffer.persistence.entity.MatchDecision;
import com.sickton.jgaffer.persistence.repository.MatchDecisionRepository;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spring service that encapsulates match data loading and recommendation logic
 * for all supported leagues.
 *
 * <p>Holds all match data in memory after startup to avoid repeated CSV parsing
 * on each request. Accepts a {@code league} string ("PL" or "SA") to route
 * requests to the correct data set.</p>
 *
 * @author sickton
 */
@Service
public class MatchService {

    public static final String PREMIER_LEAGUE = "PL";
    public static final String SERIE_A = "SA";

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

    // Premier League data
    private Map<Integer, String> plTeams;
    private Map<Integer, String> plTitles;
    private Map<String, String> plTeamCodes;
    private Map<String, FileStorage> plTeamData;
    private Map<String, MatchContext> plContextMap;

    // Serie A data
    private Map<Integer, String> saTeams;
    private Map<Integer, String> saTitles;
    private Map<String, String> saTeamCodes;
    private Map<String, FileStorage> saTeamData;
    private Map<String, MatchContext> saContextMap;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final MatchDecisionRepository decisionRepository;

    public MatchService(MatchDecisionRepository decisionRepository) {
        this.decisionRepository = decisionRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Loading Premier League data...");
        plTitles   = ApplicationParser.parseTitles("/PremierLeague/PremierLeagueMatches.csv");
        plTeamData = ApplicationParser.parseSquadInformation("/PremierLeague/SquadInformation.csv");
        plTeams    = ApplicationParser.buildTeamMapFromCsv("/PremierLeague/SquadInformation.csv");
        plTeamCodes = ApplicationParser.getTeamCodeMap("/PremierLeague/SquadInformation.csv");
        plContextMap = LeagueDataFactory.buildAllContexts(
                "/PremierLeague/MatchMinuteContext.csv", plTitles, plTeamData);
        log.info("Loaded {} PL match contexts", plContextMap.size());

        log.info("Loading Serie A data...");
        saTitles   = ApplicationParser.parseTitles("/SerieA/SerieAMatches.csv");
        saTeamData = ApplicationParser.parseSquadInformation("/SerieA/SquadInformation.csv");
        saTeams    = ApplicationParser.buildTeamMapFromCsv("/SerieA/SquadInformation.csv");
        saTeamCodes = ApplicationParser.getTeamCodeMap("/SerieA/SquadInformation.csv");
        saContextMap = LeagueDataFactory.buildAllContexts(
                "/SerieA/MatchMinuteContext.csv", saTitles, saTeamData);
        log.info("Loaded {} Serie A match contexts", saContextMap.size());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Map<Integer, String> teamsFor(String league) {
        return SERIE_A.equals(league) ? saTeams : plTeams;
    }

    private Map<Integer, String> titlesFor(String league) {
        return SERIE_A.equals(league) ? saTitles : plTitles;
    }

    private Map<String, String> teamCodesFor(String league) {
        return SERIE_A.equals(league) ? saTeamCodes : plTeamCodes;
    }

    private Map<String, FileStorage> teamDataFor(String league) {
        return SERIE_A.equals(league) ? saTeamData : plTeamData;
    }

    private Map<String, MatchContext> contextsFor(String league) {
        return SERIE_A.equals(league) ? saContextMap : plContextMap;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Returns all teams for the given league, keyed by display ID (sorted by ID). */
    public Map<Integer, String> getTeams(String league) {
        return Collections.unmodifiableMap(new TreeMap<>(teamsFor(league)));
    }

    /** Returns the team name for a given team ID in the given league. */
    public String getTeamName(String league, int teamId) {
        return teamsFor(league).get(teamId);
    }

    /** Returns the short code (e.g. "MCI") for a given team name in the given league. */
    public String getTeamCode(String league, String teamName) {
        return teamCodesFor(league).get(teamName);
    }

    /**
     * Returns fixtures for the given team split into home and away lists.
     * Each inner map has keys "id" (Integer matchId) and "title" (String match code).
     */
    public Map<String, List<Map<String, Object>>> getFixtures(String league, String teamName) {
        String code = teamCodesFor(league).get(teamName);
        Map<Integer, String> all = new TreeMap<>(
                LeagueDataFactory.getFixtureList(code, titlesFor(league)));

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
     * Fetches the pre-built MatchContext for a given league, match number, and minute.
     * Returns null if the combination is not found.
     */
    public MatchContext getMatchContext(String league, int matchNumber, int minute) {
        String matchTitle = titlesFor(league).get(matchNumber);
        if (matchTitle == null) return null;
        return contextsFor(league).get(matchTitle + "_" + minute);
    }

    /** Returns the Team object for a given team name in the given league. */
    public Team getTeam(String league, String teamName) {
        return LeagueDataFactory.buildTeamFromName(teamName, teamDataFor(league));
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

    /** Runs a full match simulation using the supplied per-phase tactic choices. */
    public SimulationResult simulate(MatchContext context,
                                     Team team,
                                     Map<Integer, Tactic> tacticPerPhase) {
        return new GameSimulator().simulate(context, team, tacticPerPhase);
    }

    public SimulationResult simulateAndLog(
            String league,
            int teamId,
            int matchId,
            int minute,
            Tactic startTactic,
            MatchContext context,
            Team team,
            Map<Integer, Tactic> tacticPerPhase) {

        // 1️⃣ Run pure simulation
        SimulationResult result = simulate(context, team, tacticPerPhase);

        // 2️⃣ Determine home/away perspective
        boolean isHome = context.getHome().getName()
                .equalsIgnoreCase(team.getName());

        int finalHomeGoals = result.getFinalHomeGoals();
        int finalAwayGoals = result.getFinalAwayGoals();

        int teamFinalGoals = isHome ? finalHomeGoals : finalAwayGoals;
        int opponentFinalGoals = isHome ? finalAwayGoals : finalHomeGoals;

        int goalDelta = teamFinalGoals - opponentFinalGoals;

        // 3️⃣ Determine outcome
        String outcome;
        if (goalDelta > 0) outcome = "WIN";
        else if (goalDelta < 0) outcome = "LOSS";
        else outcome = "DRAW";

        // 4️⃣ Determine phase label
        String phase = determinePhase(minute);

        // 5️⃣ Build MatchDecision entity
        MatchDecision decision = new MatchDecision();

        decision.setLeague(league);
        decision.setTeamId(teamId);
        decision.setMatchId(matchId);

        decision.setStartMinute(minute);
        decision.setStartTactic(startTactic.name());
        decision.setGamePhase(phase);

        decision.setFidelity(result.getTacticFidelityScore());
        decision.setMatchingPhases(result.getMatchingPhases());
        decision.setTotalPhases(result.getTotalPhases());

        decision.setFinalHomeGoals(finalHomeGoals);
        decision.setFinalAwayGoals(finalAwayGoals);

        decision.setGoalDelta(goalDelta);
        decision.setOutcome(outcome);

        decision.setCreatedAt(java.time.LocalDateTime.now());
        decision.setUserTactic(startTactic.name());

        // 6️⃣ Persist
        decisionRepository.save(decision);

        return result;
    }

    private String determinePhase(int minute) {
        if (minute <= 15) return "EARLY_MINUTES";
        if (minute <= 44) return "CLOSING_HALF";
        if (minute <= 50) return "HALF_TIME";
        if (minute <= 60) return "BUILD_PHASE";
        if (minute <= 70) return "TENSION_TIME";
        if (minute <= 87) return "LATE_GAME";
        return "STOPPAGE_TIME";
    }
}
