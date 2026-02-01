package com.sickton.jgaffer.engine;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamDrawing;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamLosing;
import com.sickton.jgaffer.rules.early_minutes.EarlyTeamWinning;
import com.sickton.jgaffer.rules.late_minutes.LateTeamDrawing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamLosing;
import com.sickton.jgaffer.rules.late_minutes.LateTeamWinning;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamDrawing;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamLosing;
import com.sickton.jgaffer.rules.middle_minutes.MiddleTeamWinning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TacticalRecommendationTest {

    Team homeTeam;
    Team awayTeam;
    @BeforeEach
    public void setUp() {
        //Liverpool Playing 11;
        Player ali = new Player("Alisson Becker", Position.GK, 3);
        Player frimp = new Player("Jeremie Frimpong", Position.DEF, 4);
        Player kerk = new Player("Milos Kerkez", Position.DEF, 3);
        Player vvd = new Player("Virgil Van Dijk", Position.DEF, 4);
        Player kon = new Player("Ibrahima Konate",  Position.DEF, 2);
        Player mac = new Player("Alexis Mac Allister", Position.MID, 3);
        Player sob = new Player("Dominic Szobozlai", Position.MID, 5);
        Player wirtz = new Player("Florian Wirtz", Position.MID, 5);
        Player hugo = new Player("Hugo Ekitike", Position.FWD, 4);
        Player salah = new Player("Mohammad Salah", Position.FWD, 3);
        Player gakpo = new Player("Cody Gakpo", Position.FWD, 2);

        //Manchester City Playing 11;
        Player don = new Player("Gianluigi Donnarumma", Position.GK, 2);
        Player ruben = new Player("Ruben Diaz", Position.DEF, 4);
        Player stones = new Player("John Stones", Position.DEF, 2);
        Player nico = new Player("Nico O'Riley", Position.DEF, 4);
        Player ait = new Player("Ryan Ait Nouri",  Position.DEF, 3);
        Player rodri = new Player("Rodri", Position.MID, 4);
        Player cherki = new Player("Rayan Cherki", Position.MID, 5);
        Player reinj = new Player("Tijjani Reinjdiers", Position.MID, 3);
        Player haaland = new Player("Erling Haaland", Position.FWD, 3);
        Player jeremy = new Player("Jeremy Doku", Position.FWD, 4);
        Player foden = new Player("Phil Foden", Position.FWD, 3);

        Map<Player, PlayerAvailability> playerA = new HashMap<Player, PlayerAvailability>();
        playerA.put(don, PlayerAvailability.AVAILABLE);
        playerA.put(ruben, PlayerAvailability.AVAILABLE);
        playerA.put(stones, PlayerAvailability.AVAILABLE);
        playerA.put(nico, PlayerAvailability.AVAILABLE);
        playerA.put(ait, PlayerAvailability.AVAILABLE);
        playerA.put(rodri, PlayerAvailability.AVAILABLE);
        playerA.put(cherki, PlayerAvailability.AVAILABLE);
        playerA.put(reinj, PlayerAvailability.AVAILABLE);
        playerA.put(haaland, PlayerAvailability.AVAILABLE);
        playerA.put(jeremy, PlayerAvailability.AVAILABLE);
        playerA.put(foden, PlayerAvailability.AVAILABLE);

        Map<Player, PlayerAvailability> playerB = new HashMap<Player, PlayerAvailability>();
        playerB.put(ali, PlayerAvailability.AVAILABLE);
        playerB.put(frimp, PlayerAvailability.AVAILABLE);
        playerB.put(kerk, PlayerAvailability.AVAILABLE);
        playerB.put(vvd, PlayerAvailability.AVAILABLE);
        playerB.put(kon, PlayerAvailability.AVAILABLE);
        playerB.put(mac, PlayerAvailability.AVAILABLE);
        playerB.put(sob, PlayerAvailability.AVAILABLE);
        playerB.put(wirtz, PlayerAvailability.AVAILABLE);
        playerB.put(hugo, PlayerAvailability.AVAILABLE);
        playerB.put(salah, PlayerAvailability.AVAILABLE);
        playerB.put(gakpo, PlayerAvailability.AVAILABLE);

        Squad livSquad = new Squad("Liverpool", playerB, "Arne Slot", Style.POSSESSION_BASED);
        Squad citySquad = new Squad("Manchester City", playerA, "Pep Guardiola", Style.POSSESSION_BASED);

        Map<Player, PlayerState> livPlayingXI = new HashMap<Player, PlayerState>();
        livPlayingXI.put(ali, new PlayerState(ali, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(frimp, new PlayerState(frimp, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(kerk, new PlayerState(kerk, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(vvd, new PlayerState(vvd, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(kon, new PlayerState(kon, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(mac, new PlayerState(mac, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(sob, new PlayerState(sob, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(wirtz, new PlayerState(wirtz, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(hugo, new PlayerState(hugo, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(salah, new PlayerState(salah, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        livPlayingXI.put(gakpo, new PlayerState(gakpo, InjuryStatus.NONE, StaminaLevel.HIGH, false));

        Map<Player, PlayerState> cityPlayingXI = new HashMap<Player, PlayerState>();
        cityPlayingXI.put(don, new PlayerState(don, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(ruben, new PlayerState(ruben, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(stones, new PlayerState(stones, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(nico, new PlayerState(nico, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(ait, new PlayerState(ait, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(rodri, new PlayerState(rodri, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(cherki, new PlayerState(cherki, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(reinj, new PlayerState(reinj, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(haaland, new PlayerState(haaland, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(jeremy, new PlayerState(jeremy, InjuryStatus.NONE, StaminaLevel.HIGH, false));
        cityPlayingXI.put(foden, new PlayerState(foden, InjuryStatus.NONE, StaminaLevel.HIGH, false));

        homeTeam = new Team(livSquad, livPlayingXI, Formation.F_4_2_3_1);
        awayTeam = new Team(citySquad, cityPlayingXI, Formation.F_4_3_3);
    }

    @Test
    public void testTacticReturned() {
        MatchContext m = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 0, 77, 45.0, 55.0);
        MatchContext mOne = new MatchContext("LIV-CITY", homeTeam, awayTeam, 1, 0, 55, 56.0, 44.0);
        MatchContext mTwo = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 2, 82, 39.0, 61.0);
        MatchContext etw = new MatchContext("LIV-CITY", homeTeam, awayTeam, 1, 0, 17, 51.0, 49.0);
        MatchContext etd = new MatchContext("LIV-CITY", homeTeam, awayTeam, 2, 2, 10, 48, 52);
        MatchContext etl = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 2, 15, 48, 52);
        MatchContext mtw = new MatchContext("LIV-CITY", homeTeam, awayTeam, 2, 0, 40, 53, 47);
        MatchContext mtd = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 0, 52, 49, 51);
        MatchContext mtl = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 2, 60, 50, 50);
        MatchContext ltw = new MatchContext("LIV-CITY", homeTeam, awayTeam, 1, 0, 85, 52, 48);
        MatchContext ltd = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 0, 82, 40, 60);
        MatchContext ltl = new MatchContext("LIV-CITY", homeTeam, awayTeam, 0, 3, 80, 32, 68);

        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();

        assertEquals(Tactic.MID_BLOCK, engine.recommend(m, homeTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(m, awayTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(mOne, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(mOne, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommend(mTwo, homeTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(mTwo, awayTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(etw, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(etw, awayTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(etd, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(etd, awayTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(etl, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(etl, awayTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(mtw, homeTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommend(mtw, awayTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(mtd, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(mtd, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommend(mtl, homeTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(mtl, awayTeam));
        assertEquals(Tactic.POSSESSION,  engine.recommend(ltw, homeTeam));
        assertEquals(Tactic.COUNTER, engine.recommend(ltw, awayTeam));
        assertEquals(Tactic.MID_BLOCK, engine.recommend(ltd, homeTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(ltd, awayTeam));
        assertEquals(Tactic.HIGH_PRESS, engine.recommend(ltl, homeTeam));
        assertEquals(Tactic.POSSESSION, engine.recommend(ltl, awayTeam));
    }
}
