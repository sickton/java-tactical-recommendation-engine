package com.sickton.jgaffer.domain;

/**
 * Enumerates the fundamental playing styles that define a team's tactical identity.
 *
 * <p>The style determines the initial bias applied to a team's {@link TeamIntent} weights
 * and is a key dimension of the {@link TacticKey} used for tactic lookups.</p>
 *
 * @author sickton
 * @see TeamIntent
 * @see TacticKey
 */
public enum Style {
    /** Favours offensive play with higher attack intent. */
    ATTACKING,
    /** Favours defensive solidity with higher defence intent. */
    DEFENSIVE,
    /** Favours possession and tempo management with higher control intent. */
    CONTROLLING;
}
