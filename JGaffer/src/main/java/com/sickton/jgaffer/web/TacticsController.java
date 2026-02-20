package com.sickton.jgaffer.web;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.SimulationResult;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.TacticRecommendation;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Spring MVC controller for the JGaffer web UI.
 *
 * <p>Handles the four-step flow: team selection → fixture selection →
 * match context display → tactic recommendation and AI explanation.</p>
 *
 * @author sickton
 */
@Controller
public class TacticsController {

    private static final Logger log = LoggerFactory.getLogger(TacticsController.class);

    private final MatchService matchService;

    public TacticsController(MatchService matchService) {
        this.matchService = matchService;
    }

    /** GET / — Team selection page. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("teams", matchService.getTeams());
        return "index";
    }

    /** GET /fixtures?teamId=X — Fixtures for the selected team. */
    @GetMapping("/fixtures")
    public String fixtures(@RequestParam int teamId, Model model) {
        String teamName = matchService.getTeamName(teamId);
        if (teamName == null) return "redirect:/";

        try {
            Map<String, List<Map<String, Object>>> fixtures = matchService.getFixtures(teamName);
            model.addAttribute("teamId", teamId);
            model.addAttribute("teamName", teamName);
            model.addAttribute("homeFixtures", fixtures.get("home"));
            model.addAttribute("awayFixtures", fixtures.get("away"));
        } catch (Exception e) {
            log.error("Error loading fixtures for team '{}': {}", teamName, e.getMessage(), e);
            throw e;
        }
        return "fixtures";
    }

    /** GET /match?matchId=X&teamId=Y — Match context at a random minute. */
    @GetMapping("/match")
    public String match(@RequestParam int matchId,
                        @RequestParam int teamId,
                        Model model) {
        String teamName = matchService.getTeamName(teamId);
        if (teamName == null) return "redirect:/";

        int minute = new Random().nextInt(90) + 1;
        MatchContext context;
        try {
            context = matchService.getMatchContext(matchId, minute);
        } catch (Exception e) {
            log.error("Error loading match context matchId={} minute={}: {}", matchId, minute, e.getMessage(), e);
            throw e;
        }

        // If exact minute not found in CSV, fall back to minute 1
        if (context == null) {
            minute = 1;
            context = matchService.getMatchContext(matchId, minute);
        }
        if (context == null) return "redirect:/fixtures?teamId=" + teamId;

        String gamePhase;
        if      (minute <= 15)  gamePhase = "Early Minutes (0-15)";
        else if (minute <= 44)  gamePhase = "Closing Half (16-44)";
        else if (minute <= 50)  gamePhase = "Half Time (45-50)";
        else if (minute <= 60)  gamePhase = "Build Phase (51-60)";
        else if (minute <= 70)  gamePhase = "Tension Time (61-70)";
        else if (minute <= 87)  gamePhase = "Late Game (71-87)";
        else                    gamePhase = "Stoppage Time (88+)";

        model.addAttribute("teamId", teamId);
        model.addAttribute("teamName", teamName);
        model.addAttribute("matchId", matchId);
        model.addAttribute("minute", minute);
        model.addAttribute("context", context);
        model.addAttribute("gamePhase", gamePhase);
        model.addAttribute("tactics", matchService.getAllTactics());
        return "match";
    }

    /**
     * POST /simulate — Starts a full match simulation.
     * The starting tactic is applied to all phases initially; the frontend JS
     * will send AJAX calls to /simulate/phase when the user picks different tactics.
     */
    @PostMapping("/simulate")
    public String simulate(@RequestParam int teamId,
                           @RequestParam int matchId,
                           @RequestParam int minute,
                           @RequestParam String userTactic,
                           Model model) {
        String teamName = matchService.getTeamName(teamId);
        if (teamName == null) return "redirect:/";

        MatchContext context = matchService.getMatchContext(matchId, minute);
        if (context == null) return "redirect:/";

        Team team = matchService.getTeam(teamName);
        Team opponent = context.getHome().getName().equalsIgnoreCase(teamName)
                ? context.getAway() : context.getHome();

        Tactic startTactic = Tactic.valueOf(userTactic.toUpperCase());

        // Build initial tactic map: same tactic for every phase
        Map<Integer, Tactic> tacticPerPhase = new HashMap<>();
        for (int i = 0; i < 7; i++) tacticPerPhase.put(i, startTactic);

        SimulationResult result = matchService.simulate(context, team, tacticPerPhase);

        boolean isHome = context.getHome().getName().equalsIgnoreCase(teamName);
        int teamGoals     = isHome ? result.getFinalHomeGoals() : result.getFinalAwayGoals();
        int opponentGoals = isHome ? result.getFinalAwayGoals() : result.getFinalHomeGoals();

        // Serialize result to JSON so Thymeleaf can embed it safely in a <script> block
        String resultJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Build a plain map so we don't need @JsonProperty on domain classes
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("tacticFidelityScore", result.getTacticFidelityScore());
            resultMap.put("managerRating",       result.getManagerRating());
            resultMap.put("matchingPhases",      result.getMatchingPhases());
            resultMap.put("totalPhases",         result.getTotalPhases());
            resultMap.put("finalHomeGoals",      result.getFinalHomeGoals());
            resultMap.put("finalAwayGoals",      result.getFinalAwayGoals());
            resultMap.put("events", result.getEvents().stream().map(ev -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("minute", ev.getMinute());
                m.put("type",   ev.getType().name());
                m.put("label",  ev.getLabel());
                return m;
            }).toList());
            resultJson = mapper.writeValueAsString(resultMap);
        } catch (Exception e) {
            resultJson = "{\"events\":[],\"tacticFidelityScore\":0,\"managerRating\":\"—\",\"finalHomeGoals\":0,\"finalAwayGoals\":0}";
        }

        model.addAttribute("teamName",       teamName);
        model.addAttribute("opponentName",   opponent.getName());
        model.addAttribute("teamId",         teamId);
        model.addAttribute("matchId",        matchId);
        model.addAttribute("startMinute",    minute);
        model.addAttribute("startTactic",    startTactic);
        model.addAttribute("startHomeGoals", context.getHomeGoals());
        model.addAttribute("startAwayGoals", context.getAwayGoals());
        model.addAttribute("isHome",         isHome);
        model.addAttribute("resultJson",     resultJson);
        model.addAttribute("teamGoals",      teamGoals);
        model.addAttribute("opponentGoals",  opponentGoals);
        model.addAttribute("tactics",        matchService.getAllTactics());
        return "simulation";
    }

    /**
     * POST /simulate/phase — AJAX endpoint called when the user picks a new tactic
     * at a phase boundary. Re-runs the simulation from the given phase onwards and
     * returns updated events as JSON.
     */
    @PostMapping("/simulate/phase")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simulatePhase(
            @RequestParam int teamId,
            @RequestParam int matchId,
            @RequestParam int fromMinute,
            @RequestParam int fromHomeGoals,
            @RequestParam int fromAwayGoals,
            @RequestParam String tacticsJson) {
        String teamName = matchService.getTeamName(teamId);
        if (teamName == null) return ResponseEntity.badRequest().build();

        MatchContext baseContext = matchService.getMatchContext(matchId, 1);
        if (baseContext == null) return ResponseEntity.badRequest().build();

        // Build a context at fromMinute with the current score
        MatchContext phaseContext = new MatchContext(
                baseContext.getTitle(),
                baseContext.getHome(),
                baseContext.getAway(),
                fromHomeGoals,
                fromAwayGoals,
                fromMinute
        );

        Team team = matchService.getTeam(teamName);

        // Parse tactic-per-phase map from JSON string, e.g. {"2":"HIGH_PRESS","3":"LOW_BLOCK"}
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
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("events", result.getEvents().stream().map(ev -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("minute", ev.getMinute());
            m.put("type",   ev.getType().name());
            m.put("label",  ev.getLabel());
            return m;
        }).toList());
        response.put("finalHomeGoals",     result.getFinalHomeGoals());
        response.put("finalAwayGoals",     result.getFinalAwayGoals());
        response.put("finalTeamGoals",     isHome ? result.getFinalHomeGoals() : result.getFinalAwayGoals());
        response.put("finalOpponentGoals", isHome ? result.getFinalAwayGoals() : result.getFinalHomeGoals());
        response.put("tacticFidelityScore",result.getTacticFidelityScore());
        response.put("managerRating",      result.getManagerRating());
        response.put("matchingPhases",     result.getMatchingPhases());
        response.put("totalPhases",        result.getTotalPhases());

        return ResponseEntity.ok(response);
    }

    /** POST /recommend — Engine recommendation + AI explanation. */
    @PostMapping("/recommend")
    public String recommend(@RequestParam int teamId,
                            @RequestParam int matchId,
                            @RequestParam int minute,
                            @RequestParam String userTactic,
                            Model model) {
        String teamName = matchService.getTeamName(teamId);
        if (teamName == null) return "redirect:/";

        MatchContext context = matchService.getMatchContext(matchId, minute);
        if (context == null) return "redirect:/";

        Team team = matchService.getTeam(teamName);
        TacticRecommendation recommendation = matchService.getRecommendation(context, team);
        Tactic userTact = Tactic.valueOf(userTactic.toUpperCase());
        boolean agrees = userTact == recommendation.getTactic();

        String explanation = matchService.getExplanation(context, team, recommendation);

        Team opponent = context.getHome().getName().equalsIgnoreCase(teamName)
                ? context.getAway() : context.getHome();

        int teamGoals = context.getHome().getName().equalsIgnoreCase(teamName)
                ? context.getHomeGoals() : context.getAwayGoals();
        int opponentGoals = context.getHome().getName().equalsIgnoreCase(teamName)
                ? context.getAwayGoals() : context.getHomeGoals();

        model.addAttribute("teamName", teamName);
        model.addAttribute("opponentName", opponent.getName());
        model.addAttribute("minute", minute);
        model.addAttribute("teamGoals", teamGoals);
        model.addAttribute("opponentGoals", opponentGoals);
        model.addAttribute("userTactic", userTact);
        model.addAttribute("recommendation", recommendation);
        model.addAttribute("agrees", agrees);
        model.addAttribute("explanation", explanation);
        return "result";
    }
}
