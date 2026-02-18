package com.sickton.jgaffer.domain;

/**
 * Pairs a tactical recommendation with a confidence score and suggested formation.
 *
 * <p>Returned by {@link com.sickton.jgaffer.engine.TacticalRecommendationEngine#recommendWithDetails}
 * to expose not just <em>what</em> tactic is recommended but <em>how certain</em> the engine
 * is about it, and which formation best supports executing that tactic. Confidence ranges
 * from 0 (final weights sit exactly on a classification boundary) to 100 (weights are at
 * the centre of their respective zones).</p>
 *
 * @see Tactic
 * @see Formation
 * @see com.sickton.jgaffer.engine.TacticalRecommendationEngine
 * @author sickton
 */
public class TacticRecommendation {
    private final Tactic tactic;
    private final int confidence;
    private final Formation suggestedFormation;

    /**
     * Constructs a new TacticRecommendation.
     *
     * @param tactic             the recommended tactic
     * @param confidence         the confidence score, 0–100
     * @param suggestedFormation the formation best suited to executing the tactic
     */
    public TacticRecommendation(Tactic tactic, int confidence, Formation suggestedFormation) {
        this.tactic = tactic;
        this.confidence = confidence;
        this.suggestedFormation = suggestedFormation;
    }

    /**
     * Returns the recommended tactic.
     *
     * @return the {@link Tactic}
     */
    public Tactic getTactic() {
        return tactic;
    }

    /**
     * Returns the confidence score for this recommendation.
     *
     * @return an integer from 0 (boundary) to 100 (zone centre)
     */
    public int getConfidence() {
        return confidence;
    }

    /**
     * Returns the formation suggested to execute this tactic.
     *
     * @return the {@link Formation} best suited to the recommended tactic
     */
    public Formation getSuggestedFormation() {
        return suggestedFormation;
    }

    @Override
    public String toString() {
        return tactic + " — " + suggestedFormation.getLabel() + " (Confidence: " + confidence + "%)";
    }
}
