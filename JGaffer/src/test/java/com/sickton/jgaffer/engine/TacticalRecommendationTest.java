package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TacticalRecommendationTest {

    private Team home;
    private Team away;

    @BeforeAll
    public void setUp()
    {
        Squad liverpool = new Squad("Liverpool","Arne Slot",Style.ATTACKING, Formation.F_4_3_3);
        home = new Team(liverpool, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        Squad realMadrid = new Squad("Real Madrid","Carlo Ancelotti",Style.CONTROLLING, Formation.F_4_3_3);
        away = new Team(realMadrid, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);
    }

    @Test
    public void testEngine()
    {
        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        MatchContext earlyGame = new MatchContext("LIV-RMD", home, away, 0, 0, 10);
        MatchContext closingHalf = new MatchContext("LIV-RMD", home, away, 0, 0, 38);
        MatchContext halfTime = new MatchContext("LIV-RMD", home, away, 1, 0, 45);
        MatchContext buildPhase = new MatchContext("LIV-RMD", home, away, 1, 0, 53);
        MatchContext tensionTime = new MatchContext("LIV-RMD", home, away, 1, 1, 66);
        MatchContext lateGame = new MatchContext("LIV-RMD", home, away, 2, 1, 80);
        MatchContext stoppageTime = new MatchContext("LIV-RMD", home, away, 2, 1, 91);

        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(earlyGame, home));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(closingHalf, home));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(halfTime, home));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(buildPhase, home));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(tensionTime, home));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(lateGame, home));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, home));

        assertEquals(Tactic.CONTROL, engine.recommendTactic(earlyGame, away));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(closingHalf, away));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(halfTime, away));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(buildPhase, away));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(tensionTime, away));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(lateGame, away));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, away));

        // Formation assertion: HIGH_PRESS → 4-3-3
        TacticRecommendation closingHalfRec = engine.recommendWithDetails(closingHalf, home);
        assertEquals(Tactic.HIGH_PRESS, closingHalfRec.getTactic());
        assertEquals(Formation.F_4_3_3, closingHalfRec.getSuggestedFormation());
    }

    @Test
    public void testCLFinal() {
        Squad interSquad = new Squad("Inter Milan", "Simone Inzaghi", Style.CONTROLLING, Formation.F_4_2_3_1);
        Team homeTeam = new Team(interSquad, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        Squad psgSquad = new Squad("PSG", "Luis Enrique", Style.ATTACKING, Formation.F_4_3_3);
        Team awayTeam = new Team(psgSquad, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        MatchContext earlyGame = new MatchContext("INT-PSG", homeTeam, awayTeam, 0, 0, 10);
        MatchContext closingHalf = new MatchContext("INT-PSG", homeTeam, awayTeam, 0, 0, 38);
        MatchContext halfTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 0, 45);
        MatchContext buildPhase = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 0, 53);
        MatchContext tensionTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 1, 66);
        MatchContext lateGame = new MatchContext("INT-PSG", homeTeam, awayTeam, 2, 1, 80);
        MatchContext stoppageTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 2, 1, 91);

        assertEquals(Tactic.CONTROL, engine.recommendTactic(earlyGame, homeTeam));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(closingHalf, homeTeam));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(halfTime, homeTeam));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(buildPhase, homeTeam));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(tensionTime, homeTeam));
        assertEquals(Tactic.CONTROL, engine.recommendTactic(lateGame, homeTeam));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, homeTeam));

        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(earlyGame, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(closingHalf, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(halfTime, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(buildPhase, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(tensionTime, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(lateGame, awayTeam));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, awayTeam));
    }

    /**
     * Verifies that the opponent's playing style influences the tactical recommendation.
     *
     * Uses a CONTROLLING team (base attack=0.28, near the 0.33 LOW/MEDIUM boundary) losing
     * 0-1 at minute 66 (TENSION_TIME). The +0.03 vs CONTROLLING opponent pushes attack into
     * MEDIUM range (→ HIGH_PRESS), while the -0.03 vs ATTACKING opponent keeps attack at
     * exactly 0.33 (LOW, → CONTROL). This boundary crossing proves that opponent-awareness
     * actually changes the tactic output.
     */
    @Test
    public void testOpponentStyleInfluencesTactic() {
        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        // CONTROLLING team: base attack=0.28, just below the LOW/MEDIUM threshold (0.33).
        // Losing deltas push dAttack=+0.08.
        // vs CONTROLLING opp: dAttack += 0.03 → total dAttack=0.11 → attack=0.39 (MEDIUM)
        // vs ATTACKING opp:   dAttack -= 0.03 → total dAttack=0.05 → attack=0.33 (LOW boundary)
        // Different IntentRange → different WeightCombination → different tactic
        Squad interSquad = new Squad("Inter", "Simone Inzaghi", Style.CONTROLLING, Formation.F_4_2_3_1);
        Team controllingTeam = new Team(interSquad, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        Squad citySquad = new Squad("Man City", "Pep Guardiola", Style.CONTROLLING, Formation.F_4_3_3);
        Team controllingOpponent = new Team(citySquad, StaminaLevel.HIGH, TeamAdaptability.HIGH);

        Squad chelseaSquad = new Squad("Chelsea", "Enzo Maresca", Style.ATTACKING, Formation.F_4_2_3_1);
        Team attackingOpponent = new Team(chelseaSquad, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        // Losing 0-1 at minute 66 (TENSION_TIME)
        MatchContext vsControllingOpp = new MatchContext("INT-MCI", controllingTeam, controllingOpponent, 0, 1, 66);
        MatchContext vsAttackingOpp   = new MatchContext("INT-CHE", controllingTeam, attackingOpponent,   0, 1, 66);

        Tactic tacticVsControlling = engine.recommendTactic(vsControllingOpp, controllingTeam);
        Tactic tacticVsAttacking   = engine.recommendTactic(vsAttackingOpp,   controllingTeam);

        // Expected: HIGH_PRESS vs CONTROLLING (attack crosses into MEDIUM), CONTROL vs ATTACKING (attack stays LOW)
        assertNotEquals(tacticVsControlling, tacticVsAttacking,
            "CONTROLLING team losing 0-1 at min 66: attack crosses LOW/MEDIUM boundary depending on opponent style");
    }
}
