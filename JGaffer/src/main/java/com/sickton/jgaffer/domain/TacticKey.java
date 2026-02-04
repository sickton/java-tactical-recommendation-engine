package com.sickton.jgaffer.domain;

import java.util.Objects;

public class TacticKey {
    private final Style style;
    private final WeightCombination weight;
    private final GamePhase phase;

    public TacticKey(Style style, WeightCombination weight, GamePhase phase) {
        this.style = style;
        this.weight = weight;
        this.phase = phase;
    }

    public Style getStyle() {
        return style;
    }

    public WeightCombination getWeight() {
        return weight;
    }

    public GamePhase getPhase() {
        return phase;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(style, weight, phase);
    }
}
