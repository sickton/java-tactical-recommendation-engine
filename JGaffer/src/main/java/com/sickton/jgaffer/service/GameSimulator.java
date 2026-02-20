package com.sickton.jgaffer.service;

import com.sickton.jgaffer.domain.*;
import com.sickton.jgaffer.engine.TacticalRecommendationEngine;

import java.util.*;

/**
 * Simulates the remainder of a football match from a given minute to full-time,
 * using the user-supplied tactic at each game phase.
 *
 * <p>Goal events are probabilistic (randomised each run). At each phase boundary
 * the engine independently evaluates what tactic it would recommend, which is used
 * to compute the tactic-fidelity score.</p>
 */
public class GameSimulator {

    /** Phase boundaries: [startMinute, endMinute, phaseName] */
    private static final int[][] PHASES = {
        {  0,  15 },
        { 16,  44 },
        { 45,  50 },
        { 51,  60 },
        { 61,  70 },
        { 71,  87 },
        { 88,  90 }
    };

    private static final String[] PHASE_NAMES = {
        "Early Minutes",
        "Closing Half",
        "Half Time",
        "Build Phase",
        "Tension Time",
        "Late Game",
        "Stoppage Time"
    };

    /**
     * Aggressiveness factor for each tactic: higher = more attack probability,
     * lower defensive probability.  [attackBonus, defenceBonus]
     */
    private static final Map<Tactic, double[]> TACTIC_BIAS = new EnumMap<>(Tactic.class);
    static {
        TACTIC_BIAS.put(Tactic.GEGENPRESSING,  new double[]{ 0.18,  0.00 });
        TACTIC_BIAS.put(Tactic.HIGH_PRESS,     new double[]{ 0.14, -0.02 });
        TACTIC_BIAS.put(Tactic.TIKI_TAKA,      new double[]{ 0.06,  0.06 });
        TACTIC_BIAS.put(Tactic.CONTROL,        new double[]{ 0.04,  0.08 });
        TACTIC_BIAS.put(Tactic.COUNTER_ATTACK, new double[]{ 0.10,  0.10 });
        TACTIC_BIAS.put(Tactic.DIRECT_PLAY,    new double[]{ 0.12,  0.02 });
        TACTIC_BIAS.put(Tactic.LOW_BLOCK,      new double[]{ 0.00,  0.18 });
    }

    private final TacticalRecommendationEngine engine = new TacticalRecommendationEngine();
    private final Random rng = new Random();

    /**
     * Runs the simulation from {@code startMinute} to full-time.
     *
     * @param context        initial match context (score + teams at startMinute)
     * @param managedTeam    the team the user is managing
     * @param tacticPerPhase user-chosen tactic for each phase (keyed by phase index 0-6).
     *                       Phases before startMinute are ignored.
     * @return full simulation result with events, final score, fidelity, and rating
     */
    public SimulationResult simulate(MatchContext context,
                                     Team managedTeam,
                                     Map<Integer, Tactic> tacticPerPhase) {
        boolean isHome = context.getHome().getName().equalsIgnoreCase(managedTeam.getName());
        int homeGoals = context.getHomeGoals();
        int awayGoals = context.getAwayGoals();
        int startMinute = context.getMinute();

        List<MatchEvent> events = new ArrayList<>();
        int matchingPhases = 0;
        int totalPhases = 0;

        for (int phaseIdx = 0; phaseIdx < PHASES.length; phaseIdx++) {
            int phaseStart = PHASES[phaseIdx][0];
            int phaseEnd   = PHASES[phaseIdx][1];

            // Skip phases that are fully before the start minute
            if (phaseEnd < startMinute) continue;

            int effectiveStart = Math.max(phaseStart, startMinute);
            int duration = phaseEnd - effectiveStart;   // minutes in this phase to simulate
            if (duration <= 0) continue;

            String phaseName = PHASE_NAMES[phaseIdx];

            // Add phase-change event
            events.add(new MatchEvent(effectiveStart, MatchEvent.Type.PHASE_CHANGE,
                    phaseName + " (" + effectiveStart + "'-" + phaseEnd + "')"));

            // Determine user tactic for this phase (fall back to first available)
            Tactic userTactic = tacticPerPhase.getOrDefault(phaseIdx,
                    tacticPerPhase.values().iterator().next());

            // Engine recommendation for this phase (for fidelity scoring)
            MatchContext phaseContext = new MatchContext(
                    context.getTitle(),
                    context.getHome(),
                    context.getAway(),
                    homeGoals,
                    awayGoals,
                    effectiveStart == 0 ? 1 : effectiveStart
            );
            Tactic engineTactic = null;
            try {
                engineTactic = engine.recommendWithDetails(phaseContext, managedTeam).getTactic();
            } catch (Exception ignored) { /* skip fidelity for this phase */ }

            totalPhases++;
            if (engineTactic != null && engineTactic == userTactic) {
                matchingPhases++;
            }
            // No TACTIC_NOTE event — engine choice is revealed only after user decides

            // Simulate goals in this phase
            double[] bias = TACTIC_BIAS.getOrDefault(userTactic, new double[]{0.08, 0.04});

            // Base probability per 15-minute block: ~1 goal per 30 min per team = 0.5 per block
            // Scaled linearly by duration / 15
            double scaleFactor = duration / 15.0;

            // Managed team attack probability
            double attackProb = (0.08 + bias[0]) * scaleFactor;
            // Opponent attack probability (reduced by tactic's defensive bonus)
            double defenceBonus = bias[1];
            double opponentAttackProb = (0.08 - defenceBonus) * scaleFactor;

            // Adjust for current scoreline (losing team pushes harder)
            int managedGoals = isHome ? homeGoals : awayGoals;
            int opponentGoals = isHome ? awayGoals : homeGoals;
            if (managedGoals < opponentGoals)  attackProb += 0.05 * scaleFactor;
            if (managedGoals > opponentGoals)  attackProb -= 0.02 * scaleFactor;
            opponentAttackProb = Math.max(0.01, opponentAttackProb);
            attackProb = Math.max(0.01, attackProb);

            // Roll for goals — could be 0, 1, or rarely 2 per phase
            int managedGoalCount = rollGoals(attackProb);
            int opponentGoalCount = rollGoals(opponentAttackProb);

            // Track used minutes within this phase so no two goals share the same minute
            Set<Integer> usedMinutes = new HashSet<>();

            // Distribute goals across phase minutes and record events
            for (int g = 0; g < managedGoalCount; g++) {
                int goalMin = uniqueMinute(effectiveStart, phaseEnd, usedMinutes);
                if (isHome) {
                    homeGoals++;
                    events.add(new MatchEvent(goalMin, MatchEvent.Type.GOAL_HOME,
                            goalMin + "' — GOAL! " + context.getHome().getName()
                            + " score! " + homeGoals + "-" + awayGoals));
                } else {
                    awayGoals++;
                    events.add(new MatchEvent(goalMin, MatchEvent.Type.GOAL_AWAY,
                            goalMin + "' — GOAL! " + context.getAway().getName()
                            + " score! " + homeGoals + "-" + awayGoals));
                }
            }
            for (int g = 0; g < opponentGoalCount; g++) {
                int goalMin = uniqueMinute(effectiveStart, phaseEnd, usedMinutes);
                if (isHome) {
                    awayGoals++;
                    events.add(new MatchEvent(goalMin, MatchEvent.Type.GOAL_AWAY,
                            goalMin + "' — Goal for " + context.getAway().getName()
                            + ". " + homeGoals + "-" + awayGoals));
                } else {
                    homeGoals++;
                    events.add(new MatchEvent(goalMin, MatchEvent.Type.GOAL_HOME,
                            goalMin + "' — Goal for " + context.getHome().getName()
                            + ". " + homeGoals + "-" + awayGoals));
                }
            }
        }

        // Sort all events by minute (goals within a phase may be out of order)
        events.sort(Comparator.comparingInt(MatchEvent::getMinute));

        // Tactical IQ score (fidelity %)
        int fidelity = totalPhases > 0 ? (matchingPhases * 100 / totalPhases) : 100;

        // Tactical IQ rating — based purely on how well the user matched the engine
        String rating = computeTacticalIQ(fidelity);

        return new SimulationResult(events, homeGoals, awayGoals,
                matchingPhases, totalPhases, fidelity, rating);
    }

    /**
     * Returns a goal minute within (effectiveStart, phaseEnd] that hasn't been used yet.
     * If all minutes in range are taken (very unlikely), returns phaseEnd as a fallback.
     */
    private int uniqueMinute(int effectiveStart, int phaseEnd, Set<Integer> used) {
        int candidate = effectiveStart + 1 + rng.nextInt(Math.max(1, phaseEnd - effectiveStart - 1));
        // Nudge forward until we find a free slot, capped at phaseEnd
        while (used.contains(candidate) && candidate < phaseEnd) candidate++;
        used.add(candidate);
        return candidate;
    }

    /** Returns 0, 1, or (rarely) 2 based on probability threshold. */
    private int rollGoals(double probability) {
        int goals = 0;
        if (rng.nextDouble() < probability) goals++;
        // Second goal much less likely
        if (rng.nextDouble() < probability * 0.35) goals++;
        return goals;
    }

    /** Tactical IQ rating — grades how well the user's choices matched the engine. */
    private String computeTacticalIQ(int fidelity) {
        if (fidelity >= 90) return "Elite Tactician";
        if (fidelity >= 75) return "Tactical Genius";
        if (fidelity >= 60) return "Solid Gaffer";
        if (fidelity >= 45) return "Work in Progress";
        if (fidelity >= 30) return "Out of Your Depth";
        return "Clueless in the Dugout";
    }
}
