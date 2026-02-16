package com.sickton.jgaffer.domain;

/**
 * Represents a tactical recommendation mapped from a specific weight combination and team style.
 *
 * <p>Acts as the value object in the tactic mapping, pairing a {@link WeightCombination}
 * and {@link Style} with the suggested {@link Tactic}. Retrieved from the mapping
 * using a {@link TacticKey}.</p>
 *
 * @author sickton
 * @see TacticKey
 * @see Tactic
 * @see com.sickton.jgaffer.utility.TacticMapper
 */
public class TacticSuggestion {
    private final WeightCombination combination;
    private final Tactic suggestedTactic;
    private final Style teamStyle;

    /**
     * Constructs a new TacticSuggestion with the given weight combination,
     * suggested tactic, and team style.
     *
     * @param combination    the weight combination that produced this suggestion
     * @param suggestedTactic the tactic being recommended
     * @param teamStyle      the team's playing style associated with this suggestion
     */
    public TacticSuggestion(WeightCombination combination, Tactic suggestedTactic, Style teamStyle) {
        this.combination = combination;
        this.suggestedTactic = suggestedTactic;
        this.teamStyle = teamStyle;
    }

    /**
     * Returns the weight combination associated with this suggestion.
     *
     * @return the {@link WeightCombination}
     */
    public WeightCombination getCombination() {
        return combination;
    }

    /**
     * Returns the suggested tactic.
     *
     * @return the recommended {@link Tactic}
     */
    public Tactic getSuggestedTactic() {
        return suggestedTactic;
    }

    /**
     * Returns the team style associated with this suggestion.
     *
     * @return the {@link Style}
     */
    public Style getTeamStyle() {
        return teamStyle;
    }
}
