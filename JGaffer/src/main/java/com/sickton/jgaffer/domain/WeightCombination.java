package com.sickton.jgaffer.domain;

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

    public boolean equals(WeightCombination o) {
        return (this.attackingRange.equals(o.attackingRange))
                && (this.defendingRange.equals(o.defendingRange))
                && (this.controllingRange.equals(o.controllingRange));
    }

}
