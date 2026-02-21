package com.sickton.jgaffer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.SimulationResult;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.TacticRecommendation;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.service.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for the JGaffer React frontend.
 * All endpoints return JSON; no Thymeleaf views used.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final MatchService matchService;

    public ApiController(MatchService matchService) {
        this.matchService = matchService;
    }

    /** GET /api/clubs?league=PL — Returns list of teams for the given league. */
    @GetMapping("/clubs")
    public Map<String, Object> clubs(@RequestParam(defaultValue = "PL") String league) {
        Map<Integer, String> teamsMap = matchService.getTeams(league);
        List<Map<String, Object>> teams = new ArrayList<>();
        teamsMap.forEach((id, name) -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", id);
            t.put("name", name);
            teams.add(t);
        });
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("league", league);
        resp.put("teams", teams);
        return resp;
    }

    /** GET /api/fixtures?teamId=X&league=PL — Returns home and away fixtures for the team. */
    @GetMapping("/fixtures")
    public ResponseEntity<Map<String, Object>> fixtures(
            @RequestParam int teamId,
            @RequestParam(defaultValue = "PL") String league) {
        String teamName = matchService.getTeamName(league, teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        Map<String, List<Map<String, Object>>> fixtures = matchService.getFixtures(league, teamName);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("teamId", teamId);
        resp.put("teamName", teamName);
        resp.put("league", league);
        resp.put("homeFixtures", fixtures.get("home"));
        resp.put("awayFixtures", fixtures.get("away"));
        return ResponseEntity.ok(resp);
    }

    /** GET /api/match?matchId=X&teamId=Y&league=PL — Returns match context at a random minute. */
    @GetMapping("/match")
    public ResponseEntity<Map<String, Object>> match(
            @RequestParam int matchId,
            @RequestParam int teamId,
            @RequestParam(defaultValue = "PL") String league) {
        String teamName = matchService.getTeamName(league, teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        int minute = new Random().nextInt(90) + 1;
        MatchContext context = matchService.getMatchContext(league, matchId, minute);
        if (context == null) {
            minute = 1;
            context = matchService.getMatchContext(league, matchId, minute);
        }
        if (context == null) return ResponseEntity.notFound().build();

        String gamePhase;
        if      (minute <= 15) gamePhase = "Early Minutes (0-15)";
        else if (minute <= 44) gamePhase = "Closing Half (16-44)";
        else if (minute <= 50) gamePhase = "Half Time (45-50)";
        else if (minute <= 60) gamePhase = "Build Phase (51-60)";
        else if (minute <= 70) gamePhase = "Tension Time (61-70)";
        else if (minute <= 87) gamePhase = "Late Game (71-87)";
        else                   gamePhase = "Stoppage Time (88+)";

        boolean isHome = context.getHome().getName().equalsIgnoreCase(teamName);
        String opponentName = isHome ? context.getAway().getName() : context.getHome().getName();

        // Serialize context into a plain map for JSON
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("title", context.getTitle());
        ctx.put("homeGoals", context.getHomeGoals());
        ctx.put("awayGoals", context.getAwayGoals());
        ctx.put("minute", context.getMinute());
        ctx.put("home", teamMap(context.getHome()));
        ctx.put("away", teamMap(context.getAway()));

        List<String> tactics = matchService.getAllTactics().stream()
                .map(Enum::name).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("teamId", teamId);
        resp.put("teamName", teamName);
        resp.put("opponentName", opponentName);
        resp.put("matchId", matchId);
        resp.put("minute", minute);
        resp.put("league", league);
        resp.put("gamePhase", gamePhase);
        resp.put("context", ctx);
        resp.put("tactics", tactics);
        resp.put("isHome", isHome);
        return ResponseEntity.ok(resp);
    }

    /** POST /api/recommend — Engine recommendation + AI explanation. */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(
            @RequestParam int teamId,
            @RequestParam int matchId,
            @RequestParam int minute,
            @RequestParam String userTactic,
            @RequestParam(defaultValue = "PL") String league) {
        String teamName = matchService.getTeamName(league, teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        MatchContext context = matchService.getMatchContext(league, matchId, minute);
        if (context == null) return ResponseEntity.notFound().build();

        Team team = matchService.getTeam(league, teamName);
        TacticRecommendation recommendation = matchService.getRecommendation(context, team);
        Tactic userTact = Tactic.valueOf(userTactic.toUpperCase());
        boolean agrees = userTact == recommendation.getTactic();
        String explanation = matchService.getExplanation(context, team, recommendation);

        boolean isHome = context.getHome().getName().equalsIgnoreCase(teamName);
        Team opponent = isHome ? context.getAway() : context.getHome();
        int teamGoals = isHome ? context.getHomeGoals() : context.getAwayGoals();
        int opponentGoals = isHome ? context.getAwayGoals() : context.getHomeGoals();

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("tactic", recommendation.getTactic().name());
        rec.put("confidence", recommendation.getConfidence());
        rec.put("formation", recommendation.getSuggestedFormation().getLabel());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("teamName", teamName);
        resp.put("opponentName", opponent.getName());
        resp.put("minute", minute);
        resp.put("teamGoals", teamGoals);
        resp.put("opponentGoals", opponentGoals);
        resp.put("userTactic", userTact.name());
        resp.put("recommendation", rec);
        resp.put("agrees", agrees);
        resp.put("explanation", explanation);
        return ResponseEntity.ok(resp);
    }

    /** POST /api/simulate — Starts a full match simulation. */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulate(
            @RequestParam int teamId,
            @RequestParam int matchId,
            @RequestParam int minute,
            @RequestParam String userTactic,
            @RequestParam(defaultValue = "PL") String league) {
        String teamName = matchService.getTeamName(league, teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        MatchContext context = matchService.getMatchContext(league, matchId, minute);
        if (context == null) return ResponseEntity.notFound().build();

        Team team = matchService.getTeam(league, teamName);
        Team opponent = context.getHome().getName().equalsIgnoreCase(teamName)
                ? context.getAway() : context.getHome();
        Tactic startTactic = Tactic.valueOf(userTactic.toUpperCase());

        Map<Integer, Tactic> tacticPerPhase = new HashMap<>();
        for (int i = 0; i < 7; i++) tacticPerPhase.put(i, startTactic);

        SimulationResult result = matchService.simulate(context, team, tacticPerPhase);
        boolean isHome = context.getHome().getName().equalsIgnoreCase(teamName);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("teamName", teamName);
        resp.put("opponentName", opponent.getName());
        resp.put("teamId", teamId);
        resp.put("matchId", matchId);
        resp.put("league", league);
        resp.put("startMinute", minute);
        resp.put("startTactic", startTactic.name());
        resp.put("startHomeGoals", context.getHomeGoals());
        resp.put("startAwayGoals", context.getAwayGoals());
        resp.put("isHome", isHome);
        resp.put("tacticFidelityScore", result.getTacticFidelityScore());
        resp.put("managerRating", result.getManagerRating());
        resp.put("matchingPhases", result.getMatchingPhases());
        resp.put("totalPhases", result.getTotalPhases());
        resp.put("finalHomeGoals", result.getFinalHomeGoals());
        resp.put("finalAwayGoals", result.getFinalAwayGoals());
        resp.put("events", result.getEvents().stream().map(ev -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("minute", ev.getMinute());
            m.put("type", ev.getType().name());
            m.put("label", ev.getLabel());
            return m;
        }).toList());
        return ResponseEntity.ok(resp);
    }

    /** POST /api/simulate/phase — Re-runs simulation from a given phase (AJAX). */
    @PostMapping("/simulate/phase")
    public ResponseEntity<Map<String, Object>> simulatePhase(
            @RequestParam int teamId,
            @RequestParam int matchId,
            @RequestParam int fromMinute,
            @RequestParam int fromHomeGoals,
            @RequestParam int fromAwayGoals,
            @RequestParam String tacticsJson,
            @RequestParam(defaultValue = "PL") String league) {
        String teamName = matchService.getTeamName(league, teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        MatchContext baseContext = matchService.getMatchContext(league, matchId, 1);
        if (baseContext == null) return ResponseEntity.badRequest().build();

        MatchContext phaseContext = new MatchContext(
                baseContext.getTitle(), baseContext.getHome(), baseContext.getAway(),
                fromHomeGoals, fromAwayGoals, fromMinute);

        Team team = matchService.getTeam(league, teamName);
        Map<Integer, Tactic> tacticPerPhase = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> raw = mapper.readValue(tacticsJson, Map.class);
            raw.forEach((k, v) -> tacticPerPhase.put(Integer.parseInt(k), Tactic.valueOf(v)));
        } catch (Exception e) {
            log.error("Failed to parse tacticsJson: {}", tacticsJson, e);
            return ResponseEntity.badRequest().build();
        }

        SimulationResult result = matchService.simulate(phaseContext, team, tacticPerPhase);
        boolean isHome = baseContext.getHome().getName().equalsIgnoreCase(teamName);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("events", result.getEvents().stream().map(ev -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("minute", ev.getMinute());
            m.put("type", ev.getType().name());
            m.put("label", ev.getLabel());
            return m;
        }).toList());
        resp.put("finalHomeGoals", result.getFinalHomeGoals());
        resp.put("finalAwayGoals", result.getFinalAwayGoals());
        resp.put("finalTeamGoals", isHome ? result.getFinalHomeGoals() : result.getFinalAwayGoals());
        resp.put("finalOpponentGoals", isHome ? result.getFinalAwayGoals() : result.getFinalHomeGoals());
        resp.put("tacticFidelityScore", result.getTacticFidelityScore());
        resp.put("managerRating", result.getManagerRating());
        resp.put("matchingPhases", result.getMatchingPhases());
        resp.put("totalPhases", result.getTotalPhases());
        return ResponseEntity.ok(resp);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> teamMap(Team t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.getName());
        m.put("staminaLevel", t.getStaminaLevel().name());
        m.put("adaptabilityLevel", t.getAdaptabilityLevel().name());
        return m;
    }
}
