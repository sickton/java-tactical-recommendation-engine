package com.sickton.jgaffer.web;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.TacticRecommendation;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.service.RagService;
import com.sickton.jgaffer.service.matches.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * REST API controller for the JGaffer React frontend.
 * All endpoints return JSON; no Thymeleaf views used.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private MatchService matchService;

    @Autowired
    private RagService ragService;

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
        resp.put("homeFormation", context.getHome().getFormation().getLabel());
        resp.put("awayFormation", context.getAway().getFormation().getLabel());
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

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> teamMap(Team t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.getName());
        m.put("staminaLevel", t.getStaminaLevel().name());
        m.put("adaptabilityLevel", t.getAdaptabilityLevel().name());
        return m;
    }

    @GetMapping("/story")
    public ResponseEntity<Object> getStory(
            @RequestParam String team,
            @RequestParam String league,
            @RequestParam String mode,
            @RequestParam String queryType
    ) {
        Object results = ragService.getStory(team, league, mode, queryType);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/explain")
    public ResponseEntity<Object> explainMoment(@RequestBody Map<String, Object> momentsData) {
        Object results =  ragService.explainMoment(momentsData);
        return ResponseEntity.ok(results);
    }
}
