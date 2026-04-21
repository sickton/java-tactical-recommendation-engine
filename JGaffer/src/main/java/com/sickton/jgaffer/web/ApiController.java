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
}
