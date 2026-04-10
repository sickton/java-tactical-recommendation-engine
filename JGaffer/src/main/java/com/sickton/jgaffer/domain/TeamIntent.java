package com.sickton.jgaffer.domain;

/**
 * Manages the tactical intent weights for a team: attack, control, and defence.
 *
 * <p>Each weight is a value between 0.0 and 1.0 representing the team's emphasis
 * on that dimension. Weights are set either from PCA-derived CSV values or, as a
 * fallback, from the squad's playing style.</p>
 *
 * @author sickton
 * @see Squad
 * @see Style
 */
public class TeamIntent {

    private double attack;
    private double control;
    private double defence;

    /**
     * Constructs a TeamIntent with explicit weights (e.g. PCA-derived).
     * Parameter order: attack, defence, control.
     *
     * @param attack  the attack weight  (0.0–1.0)
     * @param defence the defence weight (0.0–1.0)
     * @param control the control weight (0.0–1.0)
     */
    public TeamIntent(double attack, double defence, double control) {
        this.attack  = attack;
        this.defence = defence;
        this.control = control;
    }

    /**
     * Constructs a TeamIntent initialised from the squad's playing style.
     * Style-bias formula to be defined in JGaffer 2.0.
     *
     * @param s the squad whose style determines the initial weight bias
     */
    public TeamIntent(Squad s) {
        throw new UnsupportedOperationException("JGaffer 2.0 — style-bias formula not yet implemented");
    }

    public double getAttack()  { return attack; }
    public double getControl() { return control; }
    public double getDefence() { return defence; }

    @Override
    public String toString() {
        return String.format("Attack=%.2f Defence=%.2f Control=%.2f", attack, defence, control);
    }
}
