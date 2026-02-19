package com.sickton.jgaffer.web;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.TacticRecommendation;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.service.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
