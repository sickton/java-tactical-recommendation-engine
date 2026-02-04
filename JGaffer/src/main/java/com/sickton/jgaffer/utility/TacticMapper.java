package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class TacticMapper {

    protected static final int EXPECTED_FIELDS = 11;
    protected static final String FILE_NAME = "input/tactics.csv";

    public static Map<TacticKey, TacticSuggestion> mapTacticsAndWeights() {
        try {
            Scanner sc = new Scanner(new FileInputStream(FILE_NAME));
            Map<TacticKey, TacticSuggestion> map = new HashMap<>();
            int lineNumber = 0;
            while (sc.hasNextLine()) {
                lineNumber++;
                String line = sc.nextLine();
                String[] parts = line.split(",");
                if(lineNumber <= 2)
                    continue;
                else
                {
                    if(parts.length != EXPECTED_FIELDS)
                        throw new IllegalArgumentException("Invalid data row");
                    Style teamStyle = parseStyle(parts[0]);
                    IntentRange attack = parseIntent(parts[1]);
                    IntentRange control = parseIntent(parts[2]);
                    IntentRange defence = parseIntent(parts[3]);
                    WeightCombination wt = new WeightCombination(attack, defence, control);

                    Tactic earlyMinutes = parseTactic(parts[4]);
                    Tactic closingHalf = parseTactic(parts[5]);
                    Tactic halfTime = parseTactic(parts[6]);
                    Tactic buildPhase = parseTactic(parts[7]);
                    Tactic tensionTime = parseTactic(parts[8]);
                    Tactic lateGame = parseTactic(parts[9]);
                    Tactic stoppageTime = parseTactic(parts[10]);

                    TacticSuggestion early = new TacticSuggestion(wt, earlyMinutes, teamStyle);
                    TacticSuggestion closing = new TacticSuggestion(wt, closingHalf, teamStyle);
                    TacticSuggestion half = new TacticSuggestion(wt, halfTime, teamStyle);
                    TacticSuggestion build = new TacticSuggestion(wt, buildPhase, teamStyle);
                    TacticSuggestion tension = new TacticSuggestion(wt, tensionTime, teamStyle);
                    TacticSuggestion late = new TacticSuggestion(wt, lateGame, teamStyle);
                    TacticSuggestion stop = new TacticSuggestion(wt, stoppageTime, teamStyle);

                    map.put(new TacticKey(teamStyle, wt, GamePhase.EARLY_MINUTES), early);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.CLOSING_HALF), closing);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.HALF_TIME), half);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.BUILD_PHASE), build);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.TENSION_TIME), tension);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.LATE_GAME), late);
                    map.put(new TacticKey(teamStyle, wt, GamePhase.STOPPAGE_TIME), stop);
                }
            }
            sc.close();
            return map;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Style parseStyle(String line) {
        return Style.valueOf(line.trim().toUpperCase());
    }

    private static IntentRange parseIntent(String intent) {
        return IntentRange.valueOf(intent.trim().toUpperCase());
    }

    private static Tactic parseTactic(String tactic) {
        return Tactic.valueOf(tactic.trim().toUpperCase());
    }
}
