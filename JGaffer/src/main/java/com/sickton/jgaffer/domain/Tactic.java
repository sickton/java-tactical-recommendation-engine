package com.sickton.jgaffer.domain;

/**
 * Enumerates the seven tactical strategies available for recommendation.
 *
 * <p>Each constant represents a distinct football philosophy that the
 * {@link com.sickton.jgaffer.engine.TacticalRecommendationEngine} can recommend
 * based on the current match context and team attributes.</p>
 *
 * @author sickton
 * @see com.sickton.jgaffer.engine.TacticalRecommendationEngine
 */
public enum Tactic {
    /** Immediate aggressive pressure after losing possession. */
    GEGENPRESSING,
    /** Sustained pressure high up the pitch during opposition build-up. */
    HIGH_PRESS,
    /** Short, quick passing with positional rotation to maintain possession. */
    TIKI_TAKA,
    /** Structured possession with disciplined positioning and tempo management. */
    CONTROL,
    /** Rapid vertical progression exploiting transitional moments. */
    COUNTER_ATTACK,
    /** Quick advancement using long passes and minimal build-up. */
    DIRECT_PLAY,
    /** Deep defensive positioning with compact lines and space denial. */
    LOW_BLOCK,
}