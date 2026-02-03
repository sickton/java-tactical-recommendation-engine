package com.sickton.jgaffer.domain;

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

    public boolean equals(TacticKey o) {
        if(this.style.equals(o.style))
        {
            if(this.weight.equals(o.weight))
            {
                return this.phase.equals(o.phase);
            }
        }
        return false;
    }
}
