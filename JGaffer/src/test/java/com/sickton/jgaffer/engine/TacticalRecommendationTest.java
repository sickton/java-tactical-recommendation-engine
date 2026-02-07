package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TacticalRecommendationTest {

    private Team home;
    private Team away;

    @BeforeAll
    public void setUp()
    {
        Squad liverpool = new Squad("Liverpool","Arne Slot",Style.ATTACKING);
        home = new Team(liverpool,Formation.F_4_3_3, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        Squad realMadrid = new Squad("Real Madrid","Carlo Ancelotti",Style.CONTROLLING);
        away = new Team(realMadrid, Formation.F_4_3_3, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);
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
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(closingHalf, home));
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
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(lateGame, away));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, away));
    }

    @Test
    public void testCLFinal() {
        Squad interSquad = new Squad("Inter Milan", "Simone Inzaghi", Style.CONTROLLING);
        Team homeTeam = new Team(interSquad, Formation.F_5_3_2, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

        Squad psgSquad = new Squad("PSG", "Luis Enrique", Style.ATTACKING);
        Team awayTeam = new Team(psgSquad, Formation.F_4_3_3, StaminaLevel.MEDIUM, TeamAdaptability.HIGH);

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
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(closingHalf, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommendTactic(halfTime, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(buildPhase, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(tensionTime, awayTeam));
        assertEquals(Tactic.GEGENPRESSING, engine.recommendTactic(lateGame, awayTeam));
        assertEquals(Tactic.DIRECT_PLAY, engine.recommendTactic(stoppageTime, awayTeam));
    }
}
