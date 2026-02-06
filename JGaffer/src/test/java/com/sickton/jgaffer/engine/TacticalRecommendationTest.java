package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TacticalRecommendationTest {

    private Team home;
    private Team away;

    @BeforeAll
    public void setUp()
    {
        // -------- Liverpool (2025–26) --------

        Player alisson = new Player("Alisson Becker", Position.GK, 5);
        Player vanDijk = new Player("Virgil Van Dijk", Position.DEF, 5);
        Player konate  = new Player("Ibrahima Konate", Position.DEF, 4);
        Player kerkez  = new Player("Milos Kerkez", Position.DEF, 4);
        Player szobo   = new Player("Dominik Szoboszlai", Position.MID, 5);
        Player wirtz   = new Player("Florian Wirtz", Position.MID, 5);
        Player salah   = new Player("Mohamed Salah", Position.FWD, 5);
        Player ekitike = new Player("Hugo Ekitike", Position.FWD, 4);
        Player macca   = new Player("Alexis Mac Allister", Position.MID, 4);
        Player grav    = new Player("Ryan Gravenberch", Position.MID, 4);
        Player jones   = new Player("Curtis Jones", Position.MID, 3);

        Map<Player, PlayerAvailability> livAvailability = new HashMap<>();
        Map<Player, PlayerState> livXI = new HashMap<>();

        Player[] livPlayers = {
                alisson, vanDijk, konate, kerkez,
                szobo, wirtz, macca, grav, jones,
                salah, ekitike
        };

        for (Player p : livPlayers) {
            livAvailability.put(p, PlayerAvailability.AVAILABLE);
            livXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }

        Squad liverpool = new Squad("Liverpool",livAvailability,"Arne Slot",Style.ATTACKING);

        home = new Team(liverpool,livXI,Formation.F_4_3_3);

        // -------- Real Madrid (2025–26) --------

        Player courtois = new Player("Thibaut Courtois", Position.GK, 5);
        Player rudiger  = new Player("Antonio Rudiger", Position.DEF, 5);
        Player militao  = new Player("Eder Militao", Position.DEF, 4);
        Player carvajal = new Player("Dani Carvajal", Position.DEF, 4);
        Player mendy    = new Player("Ferland Mendy", Position.DEF, 4);
        Player bellingham = new Player("Jude Bellingham", Position.MID, 5);
        Player camavinga = new Player("Eduardo Camavinga", Position.MID, 5);
        Player valverde  = new Player("Federico Valverde", Position.MID, 5);
        Player vinicius  = new Player("Vinicius Jr", Position.FWD, 5);
        Player rodrygo   = new Player("Rodrygo", Position.FWD, 4);
        Player joselu    = new Player("Joselu", Position.FWD, 3);

        Map<Player, PlayerAvailability> rmAvailability = new HashMap<>();
        Map<Player, PlayerState> rmXI = new HashMap<>();

        Player[] rmPlayers = {
                courtois, rudiger, militao, carvajal, mendy,
                bellingham, camavinga, valverde,
                vinicius, rodrygo, joselu
        };

        for (Player p : rmPlayers) {
            rmAvailability.put(p, PlayerAvailability.AVAILABLE);
            rmXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }
        Squad realMadrid = new Squad("Real Madrid",rmAvailability,"Carlo Ancelotti",Style.CONTROLLING);
        away = new Team(realMadrid,rmXI,Formation.F_4_3_3);
    }

    @Test
    public void testEngine()
    {
        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        MatchContext earlyGame = new MatchContext("LIV-RMD", home, away, 0, 0, 10, 55, 45);
        MatchContext closingHalf = new MatchContext("LIV-RMD", home, away, 0, 0, 38, 52, 48);
        MatchContext halfTime = new MatchContext("LIV-RMD", home, away, 1, 0, 45, 51, 49);
        MatchContext buildPhase = new MatchContext("LIV-RMD", home, away, 1, 0, 53, 49, 51);
        MatchContext tensionTime = new MatchContext("LIV-RMD", home, away, 1, 1, 66, 50, 50);
        MatchContext lateGame = new MatchContext("LIV-RMD", home, away, 2, 1, 80, 53, 47);
        MatchContext stoppageTime = new MatchContext("LIV-RMD", home, away, 2, 1, 91, 57, 43);

        assertNotNull(engine.recommendTactic(earlyGame, home));
        assertNotNull(engine.recommendTactic(closingHalf, home));
        assertNotNull(engine.recommendTactic(halfTime, home));
        assertNotNull(engine.recommendTactic(buildPhase, home));
        assertNotNull(engine.recommendTactic(tensionTime, home));
        assertNotNull(engine.recommendTactic(lateGame, home));
        assertNotNull(engine.recommendTactic(stoppageTime, home));
    }
}
