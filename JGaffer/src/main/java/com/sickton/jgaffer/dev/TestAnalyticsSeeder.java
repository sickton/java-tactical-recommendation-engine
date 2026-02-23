package com.sickton.jgaffer.dev;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.service.matches.MatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
@Profile("test-analytics")
public class TestAnalyticsSeeder implements CommandLineRunner {

    private final MatchService matchService;
    private final Random random = new Random();

    public TestAnalyticsSeeder(MatchService matchService) {
        this.matchService = matchService;
    }

    @Override
    public void run(String... args) {

        System.out.println("Seeding 100 structured test simulations...");

        seedLeague("PL", 1, 20, 161);   // Premier League
        seedLeague("SA", 21, 40, 180);  // Serie A

        System.out.println("Test analytics seeding complete.");
    }

    private void seedLeague(String league,
                            int minTeamId,
                            int maxTeamId,
                            int matchId) {

        for (int i = 0; i < 50; i++) {

            int teamId = minTeamId + random.nextInt(maxTeamId - minTeamId + 1);
            int minute = random.nextInt(90);

            Tactic tactic = weightedTacticForLeague(league);

            MatchContext context =
                    matchService.getMatchContext(league, matchId, minute);

            if (context == null) continue;

            String teamName = matchService.getTeamName(league, teamId);
            if (teamName == null) continue;

            Team team = matchService.getTeam(league, teamName);

            Map<Integer, Tactic> tacticPerPhase = new HashMap<>();
            tacticPerPhase.put(0, tactic); // simple starting tactic

            try {
                matchService.simulateAndLog(
                        league,
                        teamId,
                        matchId,
                        minute,
                        tactic,
                        context,
                        team,
                        tacticPerPhase
                );
            } catch (Exception ignored) {}
        }
    }

    private Tactic weightedTacticForLeague(String league) {

        int r = random.nextInt(100);

        if ("PL".equals(league)) {
            if (r < 50) return Tactic.HIGH_PRESS;
            if (r < 80) return Tactic.CONTROL;
            return Tactic.LOW_BLOCK;
        }

        // Serie A weighting
        if (r < 50) return Tactic.CONTROL;
        if (r < 80) return Tactic.COUNTER_ATTACK;
        return Tactic.HIGH_PRESS;
    }
}