package com.sickton.jgaffer.domain;

/**
 * Classifies a tactical intent weight into a discrete range.
 *
 * <p>Raw intent values (0.0–1.0) are mapped to these ranges by
 * {@link com.sickton.jgaffer.rules.TacticalRule#getIntent(double)}:
 * LOW (0.0–0.33), MEDIUM (0.34–0.66), HIGH (0.67–1.0). Used as a component
 * of {@link WeightCombination} for tactic map lookups.</p>
 *
 * @author sickton
 * @see WeightCombination
 * @see com.sickton.jgaffer.rules.TacticalRule
 */
public enum IntentRange {
    HIGH,
    MEDIUM,
    LOW;
}
