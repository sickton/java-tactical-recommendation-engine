package com.sickton.jgaffer.domain;

import java.util.Objects;

/**
 * Composite lookup key used to retrieve the appropriate tactic from the tactic mapping.
 *
 * <p>Combines three dimensions that uniquely identify a tactical situation:
 * the team's {@link Style}, the current {@link WeightCombination} of intent ranges,
 * and the active {@link GamePhase}. Implements {@code equals} and {@code hashCode}
 * for use as a {@link java.util.Map} key.</p>
 *
 * @author sickton
 * @see TacticSuggestion
 * @see WeightCombination
 * @see com.sickton.jgaffer.utility.TacticMapper
 */
public class TacticKey {
    private final Style style;
    private final WeightCombination weight;
    private final GamePhase phase;

    /**
     * Constructs a new TacticKey from the given style, weight combination, and game phase.
     *
     * @param style  the team's playing style
     * @param weight the classified weight combination of intent ranges
     * @param phase  the current game phase
     */
    public TacticKey(Style style, WeightCombination weight, GamePhase phase) {
        this.style = style;
        this.weight = weight;
        this.phase = phase;
    }

    /**
     * Returns the team's playing style component of this key.
     *
     * @return the {@link Style}
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Returns the weight combination component of this key.
     *
     * @return the {@link WeightCombination}
     */
    public WeightCombination getWeight() {
        return weight;
    }

    /**
     * Returns the game phase component of this key.
     *
     * @return the {@link GamePhase}
     */
    public GamePhase getPhase() {
        return phase;
    }

    /**
     * Determines whether this TacticKey is equal to another object.
     * Two TacticKeys are equal if they share the same style, weight combination, and game phase.
     *
     * @param o the object to compare with
     * @return {@code true} if the given object is an equivalent TacticKey, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TacticKey other = (TacticKey) o;
        if(this.style.equals(other.style))
        {
            if(this.weight.equals(other.weight))
            {
                return this.phase.equals(other.phase);
            }
        }
        return false;
    }

    /**
     * Returns a hash code based on the style, weight combination, and game phase.
     *
     * @return the hash code for this TacticKey
     */
    @Override
    public int hashCode() {
        return Objects.hash(style, weight, phase);
    }
}
