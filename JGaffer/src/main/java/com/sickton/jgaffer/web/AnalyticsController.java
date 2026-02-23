package com.sickton.jgaffer.web;

import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.persistence.dto.LeagueNormalizerResult;
import com.sickton.jgaffer.persistence.dto.PhaseNormalizerResult;
import com.sickton.jgaffer.persistence.dto.TacticAnalytics;
import com.sickton.jgaffer.service.analytics.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/win-rate-by-tactic")
    public List<TacticAnalytics> getWinRateByTactic(
            @RequestParam(defaultValue = "PL") String league) {
        return analyticsService.getWinRateByTactic(league);
    }

    @GetMapping("/phase-normalizer")
    public PhaseNormalizerResult phaseNormalizer(
            @RequestParam String league,
            @RequestParam String tactic,
            @RequestParam(defaultValue = "2") int samplesPerTeam,
            @RequestParam(defaultValue = "2") int iterationsPerSample) {

        return analyticsService.runPhaseNormalizer(
                league,
                parseTactic(tactic),
                samplesPerTeam,
                iterationsPerSample
        );
    }

    @GetMapping("/league-normalizer")
    public LeagueNormalizerResult leagueNormalizer(
            @RequestParam String league,
            @RequestParam String tactic,
            @RequestParam(defaultValue = "3") int samplesPerTeam,
            @RequestParam(defaultValue = "3") int iterationsPerSample) {

        return analyticsService.runLeagueNormalizer(
                league,
                parseTactic(tactic),
                samplesPerTeam,
                iterationsPerSample
        );
    }

    private Tactic parseTactic(String tactic) {
        return Tactic.valueOf(tactic.toUpperCase());
    }
}