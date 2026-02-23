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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

@Service
public class AnalyticsService {

    private final MatchDecisionRepository repository;
    private final MatchService matchService;

    public AnalyticsService(MatchDecisionRepository repository,
                            MatchService matchService) {
        this.repository   = repository;
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
                            stat.getLosses(),
                            stat.getDraws(),
                            round(winRate)
                    );
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // League Normalizer — parallel Monte Carlo sweep
    //   20 teams × samplesPerTeam × iterationsPerSample trials
    //   at default params (15 × 9): n = 2700 → margin ±1.88% @ 95% CI
    // ─────────────────────────────────────────────────────────

    public LeagueNormalizerResult runLeagueNormalizer(
            String league,
            Tactic tactic,
            int samplesPerTeam,
            int iterationsPerSample) {

        Map<Integer, String> teams    = matchService.getTeams(league);
        List<Integer>        matchIds = List.copyOf(matchService.getAllMatchIds(league));

        LongAdder wins   = new LongAdder();
        LongAdder losses = new LongAdder();
        LongAdder draws  = new LongAdder();

        // Parallelise over all 20 teams; ThreadLocalRandom is per-thread — no contention
        teams.keySet().parallelStream().forEach(teamId -> {
            ThreadLocalRandom rng      = ThreadLocalRandom.current();
            String            teamName = matchService.getTeamName(league, teamId);
            Team              team     = matchService.getTeam(league, teamName);

            for (int s = 0; s < samplesPerTeam; s++) {
                int          matchId = matchIds.get(rng.nextInt(matchIds.size()));
                int          minute  = rng.nextInt(90);
                MatchContext context = matchService.getMatchContext(league, matchId, minute);
                if (context == null) continue;

                Map<Integer, Tactic> tacticMap = new HashMap<>();
                tacticMap.put(0, tactic);

                for (int i = 0; i < iterationsPerSample; i++) {
                    SimulationResult result = matchService.simulate(context, team, tacticMap);
                    int delta = calculateGoalDelta(result, context, team);
                    if      (delta > 0) wins.increment();
                    else if (delta < 0) losses.increment();
                    else                draws.increment();
                }
            }
        });

        long w = wins.longValue(), l = losses.longValue(), d = draws.longValue();
        long total    = w + l + d;
        double winRate = total == 0 ? 0 : (w * 100.0) / total;

        return new LeagueNormalizerResult(
                league, tactic.name(),
                (int) total, (int) w, (int) l, (int) d,
                round(winRate));
    }

    // ─────────────────────────────────────────────────────────
    // Phase Normalizer — parallel Monte Carlo sweep
    //   20 teams × 7 phases = 140 work items processed in parallel
    //   per-phase trials at default params: 20 × 15 × 9 = 2700 → margin ±1.88% @ 95% CI
    // ─────────────────────────────────────────────────────────

    public PhaseNormalizerResult runPhaseNormalizer(
            String league,
            Tactic tactic,
            int samplesPerTeam,
            int iterationsPerSample) {

        List<String> phases = List.of(
                "EARLY_MINUTES", "CLOSING_HALF", "HALF_TIME",
                "BUILD_PHASE",   "TENSION_TIME", "LATE_GAME", "STOPPAGE_TIME");

        Map<Integer, String> teams    = matchService.getTeams(league);
        List<Integer>        matchIds = List.copyOf(matchService.getAllMatchIds(league));

        // LongAdder[0]=wins, [1]=losses, [2]=draws — one triple per phase
        Map<String, LongAdder[]> phaseStats = new ConcurrentHashMap<>();
        phases.forEach(p -> phaseStats.put(p,
                new LongAdder[]{new LongAdder(), new LongAdder(), new LongAdder()}));

        // 20 teams × 7 phases = 140 work items; the JVM parallel pool saturates all cores
        List<int[]> workItems = new ArrayList<>(teams.size() * phases.size());
        for (Integer teamId : teams.keySet()) {
            for (int pi = 0; pi < phases.size(); pi++) {
                workItems.add(new int[]{teamId, pi});
            }
        }

        workItems.parallelStream().forEach(item -> {
            int               teamId   = item[0];
            String            phase    = phases.get(item[1]);
            ThreadLocalRandom rng      = ThreadLocalRandom.current();
            String            teamName = matchService.getTeamName(league, teamId);
            Team              team     = matchService.getTeam(league, teamName);
            LongAdder[]       acc      = phaseStats.get(phase);

            for (int s = 0; s < samplesPerTeam; s++) {
                int          matchId = matchIds.get(rng.nextInt(matchIds.size()));
                int          minute  = minuteForPhase(phase, rng);
                MatchContext context = matchService.getMatchContext(league, matchId, minute);
                if (context == null) continue;

                Map<Integer, Tactic> tacticMap = new HashMap<>();
                tacticMap.put(0, tactic);

                for (int i = 0; i < iterationsPerSample; i++) {
                    SimulationResult result = matchService.simulate(context, team, tacticMap);
                    int delta = calculateGoalDelta(result, context, team);
                    if      (delta > 0) acc[0].increment();
                    else if (delta < 0) acc[1].increment();
                    else                acc[2].increment();
                }
            }
        });

        List<PhasePerformance> results = phases.stream()
                .map(phase -> {
                    LongAdder[] acc = phaseStats.get(phase);
                    long w = acc[0].longValue(), l = acc[1].longValue(), d = acc[2].longValue();
                    long total     = w + l + d;
                    double winRate = total == 0 ? 0 : (w * 100.0) / total;
                    return new PhasePerformance(
                            phase, (int) total, (int) w, (int) l, (int) d, round(winRate));
                })
                .toList();

        return new PhaseNormalizerResult(league, tactic.name(), results);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private int calculateGoalDelta(SimulationResult result,
                                   MatchContext context,
                                   Team team) {
        boolean isHome    = context.getHome().getName().equalsIgnoreCase(team.getName());
        int teamGoals     = isHome ? result.getFinalHomeGoals() : result.getFinalAwayGoals();
        int opponentGoals = isHome ? result.getFinalAwayGoals() : result.getFinalHomeGoals();
        return teamGoals - opponentGoals;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Receives the caller's ThreadLocalRandom — no shared state
    private int minuteForPhase(String phase, ThreadLocalRandom rng) {
        return switch (phase) {
            case "EARLY_MINUTES" -> rng.nextInt(16);
            case "CLOSING_HALF"  -> 16 + rng.nextInt(29);
            case "HALF_TIME"     -> 45 + rng.nextInt(6);
            case "BUILD_PHASE"   -> 51 + rng.nextInt(10);
            case "TENSION_TIME"  -> 61 + rng.nextInt(10);
            case "LATE_GAME"     -> 71 + rng.nextInt(17);
            case "STOPPAGE_TIME" -> 88 + rng.nextInt(5);
            default -> 0;
        };
    }
}
