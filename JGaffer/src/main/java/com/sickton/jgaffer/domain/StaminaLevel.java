package com.sickton.jgaffer.domain;

/**
 * Represents a team's overall stamina level.
 *
 * <p>Used by game phase rules to scale weight adjustments — teams with lower stamina
 * receive dampened tactical shifts, while high-stamina teams can sustain more
 * aggressive changes.</p>
 *
 * @author sickton
 * @see Team
 */
public enum StaminaLevel {
    LOW,
    MEDIUM,
    HIGH;
}
