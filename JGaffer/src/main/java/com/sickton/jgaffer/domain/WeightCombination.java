package com.sickton.jgaffer.domain;

import java.util.Objects;

/**
 * Represents a classified combination of tactical intent ranges for attack, defence, and control.
 *
 * <p>Each dimension is categorized as {@link IntentRange#LOW}, {@link IntentRange#MEDIUM},
 * or {@link IntentRange#HIGH}. Used as part of a {@link TacticKey} to look up the appropriate
 * tactic from the CSV-driven mapping. Implements {@code equals} and {@code hashCode}
 * for reliable map lookups.</p>
 *
 * @author sickton
 * @see IntentRange
 * @see TacticKey
 */
public class WeightCombination {
    private final IntentRange attackingRange;
    private final IntentRange defendingRange;
    private final IntentRange controllingRange;

    /**
     * Constructs a new WeightCombination with the given intent ranges for
     * attack, defence, and control.
     *
     * @param attackingRange    the classified attacking intent range
     * @param defendingRange    the classified defending intent range
     * @param controllingRange  the classified controlling intent range
     */
    public WeightCombination(IntentRange attackingRange, IntentRange defendingRange, IntentRange controllingRange) {
        this.attackingRange = attackingRange;
        this.defendingRange = defendingRange;
        this.controllingRange = controllingRange;
    }

    /**
     * Returns the attacking intent range.
     *
     * @return the attacking {@link IntentRange}
     */
    public IntentRange getAttackingRange() {
        return attackingRange;
    }

    /**
     * Returns the defending intent range.
     *
     * @return the defending {@link IntentRange}
     */
    public IntentRange getDefendingRange() {
        return defendingRange;
    }

    /**
     * Returns the controlling intent range.
     *
     * @return the controlling {@link IntentRange}
     */
    public IntentRange getControllingRange() {
        return controllingRange;
    }

    /**
     * Determines whether this WeightCombination is equal to another object.
     * Two WeightCombinations are equal if they share the same attacking, defending,
     * and controlling intent ranges.
     *
     * @param o the object to compare with
     * @return {@code true} if the given object is an equivalent WeightCombination, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeightCombination that = (WeightCombination) o;
        return (this.attackingRange.equals(that.attackingRange))
                && (this.defendingRange.equals(that.defendingRange))
                && (this.controllingRange.equals(that.controllingRange));
    }

    /**
     * Returns a hash code based on the attacking, controlling, and defending ranges.
     *
     * @return the hash code for this WeightCombination
     */
    @Override
    public int hashCode() {
        return Objects.hash(attackingRange, controllingRange, defendingRange);
    }
}
