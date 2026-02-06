package com.sickton.jgaffer.domain;

import java.util.Objects;

public class WeightCombination {
    private final IntentRange attackingRange;
    private final IntentRange defendingRange;
    private final IntentRange controllingRange;

    public WeightCombination(IntentRange attackingRange, IntentRange defendingRange, IntentRange controllingRange) {
        this.attackingRange = attackingRange;
        this.defendingRange = defendingRange;
        this.controllingRange = controllingRange;
    }

    public IntentRange getAttackingRange() {
        return attackingRange;
    }

    public IntentRange getDefendingRange() {
        return defendingRange;
    }

    public IntentRange getControllingRange() {
        return controllingRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeightCombination that = (WeightCombination) o;
        return (this.attackingRange.equals(that.attackingRange))
                && (this.defendingRange.equals(that.defendingRange))
                && (this.controllingRange.equals(that.controllingRange));
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingRange, controllingRange, defendingRange);
    }
}
