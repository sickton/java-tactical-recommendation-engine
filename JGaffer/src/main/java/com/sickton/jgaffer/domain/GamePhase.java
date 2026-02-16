package com.sickton.jgaffer.domain;

/**
 * Enumerates the seven phases of a football match used for tactical evaluation.
 *
 * <p>Each phase corresponds to a specific minute range and has distinct tactical
 * characteristics. The {@link com.sickton.jgaffer.rules.TacticalRule} implementations
 * use these phases to apply phase-specific weight adjustments.</p>
 *
 * @author sickton
 * @see com.sickton.jgaffer.rules.TacticalRule
 */
public enum GamePhase {
    /** Minutes 0-15: Establishing structure and assessing opponent shape. */
    EARLY_MINUTES,
    /** Minutes 16-44: Intensifying adjustments as halftime approaches. */
    CLOSING_HALF,
    /** Minutes 45-50: Strategic recalibration window at the break. */
    HALF_TIME,
    /** Minutes 51-60: Implementing and evaluating halftime adjustments. */
    BUILD_PHASE,
    /** Minutes 61-70: Managing fatigue and increasing match volatility. */
    TENSION_TIME,
    /** Minutes 71-87: High urgency, scoreline-driven tactical shifts. */
    LATE_GAME,
    /** Minutes 88+: Extreme, outcome-driven decisions under maximum pressure. */
    STOPPAGE_TIME;
}
