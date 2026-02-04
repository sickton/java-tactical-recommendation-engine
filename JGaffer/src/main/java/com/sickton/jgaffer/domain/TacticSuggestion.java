package com.sickton.jgaffer.domain;

public class TacticSuggestion {
    private final WeightCombination combination;
    private final Tactic suggestedTactic;
    private final Style teamStyle;

    public TacticSuggestion(WeightCombination combination, Tactic suggestedTactic, Style teamStyle) {
        this.combination = combination;
        this.suggestedTactic = suggestedTactic;
        this.teamStyle = teamStyle;
    }

    public WeightCombination getCombination() {
        return combination;
    }

    public Tactic getSuggestedTactic() {
        return suggestedTactic;
    }

    public Style getTeamStyle() {
        return teamStyle;
    }
}
