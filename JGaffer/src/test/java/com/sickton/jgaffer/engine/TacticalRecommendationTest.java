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

        Map<Player, PlayerState> livXI = new HashMap<>();

        Player[] livPlayers = {
                alisson, vanDijk, konate, kerkez,
                szobo, wirtz, macca, grav, jones,
                salah, ekitike
        };

        for (Player p : livPlayers) {
            livXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }

        Squad liverpool = new Squad("Liverpool","Arne Slot",Style.ATTACKING);

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

        Map<Player, PlayerState> rmXI = new HashMap<>();

        Player[] rmPlayers = {
                courtois, rudiger, militao, carvajal, mendy,
                bellingham, camavinga, valverde,
                vinicius, rodrygo, joselu
        };

        for (Player p : rmPlayers) {
            rmXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }
        Squad realMadrid = new Squad("Real Madrid","Carlo Ancelotti",Style.CONTROLLING);
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
        Player sommer = new Player("Yann Sommer", Position.GK, 4);
        Player pavard = new Player("Benjamin Pavard", Position.DEF, 4);
        Player bastoni = new Player("Alessandro Bastoni", Position.DEF, 5);
        Player acerbi = new Player("Francesco Acerbi", Position.DEF, 4);
        Player barella = new Player("Nicolo Barella", Position.MID, 5);
        Player hakan = new Player("Hakan Calhanoglu", Position.MID, 5);
        Player dimarco = new Player("Federico Dimarco", Position.MID, 4);
        Player darmian = new Player("Matteo Darmian", Position.MID, 3);
        Player mkhitaryan = new Player("Henrikh Mkhitaryan", Position.MID, 4);
        Player lautaro = new Player("Lautaro Martinez", Position.FWD, 5);
        Player thuram = new Player("Marcus Thuram", Position.FWD, 4);

        Map<Player, PlayerState> interXI = new HashMap<>();

        Player[] interPlayers = {sommer, pavard, bastoni, acerbi, barella, hakan, dimarco, darmian, mkhitaryan, lautaro, thuram};

        for (Player p : interPlayers) {
            interXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }

        Squad interSquad = new Squad("Inter Milan", "Simone Inzaghi", Style.CONTROLLING);
        Team homeTeam = new Team(interSquad, interXI, Formation.F_5_3_2);

        // -------- PSG (Attacking/Direct) --------
        Player donnarumma = new Player("Gianluigi Donnarumma", Position.GK, 5);
        Player marquinhos = new Player("Marquinhos", Position.DEF, 5);
        Player pacho = new Player("Willian Pacho", Position.DEF, 4);
        Player hakimi = new Player("Achraf Hakimi", Position.DEF, 5);
        Player nuno = new Player("Nuno Mendes", Position.DEF, 4);
        Player vitinha = new Player("Vitinha", Position.MID, 4);
        Player zairEmery = new Player("Warren Zaire-Emery", Position.MID, 4);
        Player neves = new Player("Joao Neves", Position.MID, 4);
        Player dembele = new Player("Ousmane Dembele", Position.FWD, 5);
        Player barcola = new Player("Bradley Barcola", Position.FWD, 4);
        Player ramos = new Player("Goncalo Ramos", Position.FWD, 4);

        Map<Player, PlayerState> psgXI = new HashMap<>();

        Player[] psgPlayers = {donnarumma, marquinhos, pacho, hakimi, nuno, vitinha, zairEmery, neves, dembele, barcola, ramos};

        for (Player p : psgPlayers) {
            psgXI.put(p, new PlayerState(p, InjuryStatus.NONE, StaminaLevel.MEDIUM, false));
        }

        Squad psgSquad = new Squad("PSG", "Luis Enrique", Style.ATTACKING);
        Team awayTeam = new Team(psgSquad, psgXI, Formation.F_4_3_3);

        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        MatchContext earlyGame = new MatchContext("INT-PSG", homeTeam, awayTeam, 0, 0, 10, 45, 55);
        MatchContext closingHalf = new MatchContext("INT-PSG", homeTeam, awayTeam, 0, 0, 38, 48, 52);
        MatchContext halfTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 0, 45, 49, 51);
        MatchContext buildPhase = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 0, 53, 40, 60);
        MatchContext tensionTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 1, 1, 66, 50, 50);
        MatchContext lateGame = new MatchContext("INT-PSG", homeTeam, awayTeam, 2, 1, 80, 43, 57);
        MatchContext stoppageTime = new MatchContext("INT-PSG", homeTeam, awayTeam, 2, 1, 91, 35, 65);

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
