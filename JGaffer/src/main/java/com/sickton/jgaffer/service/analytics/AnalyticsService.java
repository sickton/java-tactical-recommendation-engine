package com.sickton.jgaffer.service.analytics;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.SimulationResult;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.persistence.dto.LeagueNormalizerResult;
import com.sickton.jgaffer.persistence.dto.PhaseNormalizerResult;
import com.sickton.jgaffer.persistence.dto.PhasePerformance;
import com.sickton.jgaffer.persistence.dto.TacticAnalytics;
import com.sickton.jgaffer.persistence.repository.MatchDecisionRepository;
import com.sickton.jgaffer.service.matches.MatchService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalyticsService {

    private final MatchDecisionRepository repository;
    private final MatchService matchService;
    private final Random random = new Random();

    public AnalyticsService(MatchDecisionRepository repository,
                            MatchService matchService) {
        this.repository = repository;
        this.matchService = matchService;
    }

    // ─────────────────────────────────────────────────────────
    // Historical Analytics
    // ─────────────────────────────────────────────────────────

    public List<TacticAnalytics> getWinRateByTactic(String league) {

        return repository.getWinRateByTactic(league)
                .stream()
                .map(stat -> {

                    double winRate = stat.getTotal() == 0
                            ? 0
                            : (stat.getWins() * 100.0) / stat.getTotal();

                    return new TacticAnalytics(
                            stat.getStartTactic(),
                            stat.getTotal(),
                            stat.getWins(),
                            round(winRate)
                    );
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // League Normalizer (Monte Carlo Sweep)
    // ─────────────────────────────────────────────────────────

    public LeagueNormalizerResult runLeagueNormalizer(
            String league,
            Tactic tactic,
            int samplesPerTeam,
            int iterationsPerSample) {

        int wins = 0;
        int losses = 0;
        int draws = 0;

        Map<Integer, String> teams = matchService.getTeams(league);
        List<Integer> matchIds = new ArrayList<>(matchService.getAllMatchIds(league));

        for (Integer teamId : teams.keySet()) {

            String teamName = matchService.getTeamName(league, teamId);
            Team team = matchService.getTeam(league, teamName);

            for (int s = 0; s < samplesPerTeam; s++) {

                int matchId = matchIds.get(random.nextInt(matchIds.size()));
                int minute = random.nextInt(90);

                MatchContext context =
                        matchService.getMatchContext(league, matchId, minute);

                if (context == null) continue;

                Map<Integer, Tactic> tacticMap = new HashMap<>();
                tacticMap.put(0, tactic);

                for (int i = 0; i < iterationsPerSample; i++) {

                    SimulationResult result =
                            matchService.simulate(context, team, tacticMap);

                    int delta = calculateGoalDelta(result, context, team);

                    if (delta > 0) wins++;
                    else if (delta < 0) losses++;
                    else draws++;
                }
            }
        }

        int total = wins + losses + draws;
        double winRate = total == 0 ? 0 : (wins * 100.0) / total;

        return new LeagueNormalizerResult(
                league,
                tactic.name(),
                total,
                wins,
                losses,
                draws,
                round(winRate)
        );
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private int calculateGoalDelta(SimulationResult result,
                                   MatchContext context,
                                   Team team) {

        boolean isHome = context.getHome().getName()
                .equalsIgnoreCase(team.getName());

        int teamGoals = isHome
                ? result.getFinalHomeGoals()
                : result.getFinalAwayGoals();

        int opponentGoals = isHome
                ? result.getFinalAwayGoals()
                : result.getFinalHomeGoals();

        return teamGoals - opponentGoals;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public PhaseNormalizerResult runPhaseNormalizer(
            String league,
            Tactic tactic,
            int samplesPerTeam,
            int iterationsPerSample) {

        Map<String, PhaseAccumulator> phaseStats = new LinkedHashMap<>();

        // Initialize all 7 phases
        List<String> phases = List.of(
                "EARLY_MINUTES",
                "CLOSING_HALF",
                "HALF_TIME",
                "BUILD_PHASE",
                "TENSION_TIME",
                "LATE_GAME",
                "STOPPAGE_TIME"
        );

        phases.forEach(p -> phaseStats.put(p, new PhaseAccumulator()));

        Map<Integer, String> teams = matchService.getTeams(league);
        List<Integer> matchIds =
                new ArrayList<>(matchService.getAllMatchIds(league));

        for (Integer teamId : teams.keySet()) {

            String teamName = matchService.getTeamName(league, teamId);
            Team team = matchService.getTeam(league, teamName);

            for (String phase : phases) {

                for (int s = 0; s < samplesPerTeam; s++) {

                    int matchId = matchIds.get(random.nextInt(matchIds.size()));
                    int minute = minuteForPhase(phase);

                    MatchContext context =
                            matchService.getMatchContext(league, matchId, minute);

                    if (context == null) continue;

                    Map<Integer, Tactic> tacticMap = new HashMap<>();
                    tacticMap.put(0, tactic);

                    for (int i = 0; i < iterationsPerSample; i++) {

                        SimulationResult result =
                                matchService.simulate(context, team, tacticMap);

                        int delta = calculateGoalDelta(result, context, team);

                        PhaseAccumulator acc = phaseStats.get(phase);

                        if (delta > 0) acc.wins++;
                        else if (delta < 0) acc.losses++;
                        else acc.draws++;
                    }
                }
            }
        }

        List<PhasePerformance> results = new ArrayList<>();

        for (String phase : phases) {

            PhaseAccumulator acc = phaseStats.get(phase);
            int total = acc.wins + acc.losses + acc.draws;

            double winRate = total == 0 ? 0 :
                    (acc.wins * 100.0) / total;

            results.add(new PhasePerformance(
                    phase,
                    total,
                    acc.wins,
                    acc.losses,
                    acc.draws,
                    round(winRate)
            ));
        }

        return new PhaseNormalizerResult(
                league,
                tactic.name(),
                results
        );
    }

    private static class PhaseAccumulator {
        int wins = 0;
        int losses = 0;
        int draws = 0;
    }

    private int minuteForPhase(String phase) {

        return switch (phase) {
            case "EARLY_MINUTES" -> random.nextInt(16);
            case "CLOSING_HALF" -> 16 + random.nextInt(29);
            case "HALF_TIME" -> 45 + random.nextInt(6);
            case "BUILD_PHASE" -> 51 + random.nextInt(10);
            case "TENSION_TIME" -> 61 + random.nextInt(10);
            case "LATE_GAME" -> 71 + random.nextInt(17);
            case "STOPPAGE_TIME" -> 88 + random.nextInt(5);
            default -> 0;
        };
    }
}