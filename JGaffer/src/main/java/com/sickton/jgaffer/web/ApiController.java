package com.sickton.jgaffer.web;

import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.service.RagService;
import com.sickton.jgaffer.service.matches.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API controller for the active JGaffer frontend flow.
 *
 * <p>The current product path is league selection, club selection, moment discovery,
 * moment explanation, and the tactical puzzle. Legacy fixture/match/recommendation
 * endpoints have been removed.</p>
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private RagService ragService;

    @GetMapping("/clubs")
    public Map<String, Object> clubs(@RequestParam(defaultValue = "PL") String league) {
        Map<Integer, String> teamsMap = matchService.getTeams(league);
        var teams = new ArrayList<Map<String, Object>>();
        teamsMap.forEach((id, name) -> {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("id", id);
            team.put("name", name);
            teams.add(team);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("league", league);
        response.put("teams", teams);
        return response;
    }

    @GetMapping("/story")
    public ResponseEntity<Object> getStory(
            @RequestParam String team,
            @RequestParam String league,
            @RequestParam String mode,
            @RequestParam String queryType
    ) {
        return ResponseEntity.ok(ragService.getStory(team, league, mode, queryType));
    }

    @PostMapping("/explain")
    public ResponseEntity<Object> explainMoment(@RequestBody Map<String, Object> momentData) {
        return ResponseEntity.ok(ragService.explainMoment(momentData));
    }

    @GetMapping("/network")
    public ResponseEntity<Object> getNetwork(
            @RequestParam String escapingTeam,
            @RequestParam String pressingTeam,
            @RequestParam(defaultValue = "PL") String league,
            @RequestParam int minute,
            @RequestParam(defaultValue = "0") int homeGoals,
            @RequestParam(defaultValue = "0") int awayGoals) {

        Team escaping = matchService.getTeam(league, escapingTeam);
        Team pressing = matchService.getTeam(league, pressingTeam);

        if (escaping == null || pressing == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "One or both teams not found in league: " + league)
            );
        }

        return ResponseEntity.ok(ragService.getNetwork(
                escapingTeam,
                escaping.getFormation().name(),
                pressingTeam,
                pressing.getFormation().name(),
                league,
                minute,
                homeGoals,
                awayGoals
        ));
    }

    /**
     * POST /api/puzzle
     *
     * Looks up both team formations from squad data, injects them into the
     * request body, then proxies to FastAPI /puzzle which selects the puzzle
     * type and returns the full config.
     */
    @PostMapping("/puzzle")
    public ResponseEntity<Object> getPuzzle(@RequestBody Map<String, Object> body) {
        // Extract fields needed for formation lookup
        @SuppressWarnings("unchecked")
        Map<String, Object> moment = (Map<String, Object>) body.get("moment");
        String league       = (String) body.getOrDefault("league", "PL");
        String pressingTeam = (String) body.getOrDefault("pressing_team", "");
        String escapingTeam = moment != null ? (String) moment.get("team") : "";

        Team escaping = matchService.getTeam(league, escapingTeam);
        Team pressing = matchService.getTeam(league, pressingTeam);

        // Inject formations — fall back to a default if team not found
        String escapingFormation = escaping != null ? escaping.getFormation().name() : "F_4_3_3";
        String pressingFormation = pressing != null ? pressing.getFormation().name() : "F_4_3_3";

        Map<String, Object> enriched = new java.util.HashMap<>(body);
        enriched.put("escaping_formation", escapingFormation);
        enriched.put("pressing_formation", pressingFormation);

        return ResponseEntity.ok(ragService.getPuzzle(enriched));
    }

    /**
     * POST /api/evaluate
     *
     * Proxies the user's puzzle answer straight to FastAPI /evaluate.
     * FastAPI owns all scoring, optimal path computation, and coaching.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<Object> evaluatePuzzle(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ragService.evaluatePuzzle(body));
    }
}
