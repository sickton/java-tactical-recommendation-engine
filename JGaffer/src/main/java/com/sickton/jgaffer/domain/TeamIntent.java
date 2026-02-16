package com.sickton.jgaffer.domain;

/**
 * Manages the tactical intent weights for a team: attack, control, and defence.
 *
 * <p>Each weight is a value between 0.0 and 1.0 representing the team's emphasis on that
 * dimension. Initial weights are set to an equal baseline ({@value INITIAL_VALUE}) and then
 * biased based on the squad's {@link Style} — attacking teams receive a boost to attack,
 * controlling teams to control, and defensive teams to defence.</p>
 *
 * <p>These weights serve as the starting point for game-phase-specific adjustments
 * performed by {@link com.sickton.jgaffer.rules.TacticalRule} implementations.</p>
 *
 * @author sickton
 * @see Squad
 * @see Style
 */
public class TeamIntent {
    protected static final double STYLE_BIAS = 0.1;
    protected static final double STYLE_DEDUCTION = 0.05;
    protected static final double INITIAL_VALUE = 0.33;

    private double attack;
    private double control;
    private double defence;

    /**
     * Constructs a new TeamIntent and initializes the weights based on the
     * squad's playing style.
     *
     * @param s the squad whose style determines the initial weight bias
     */
    public TeamIntent(Squad s) {
        assignWeights(s);
    }

    /**
     * Assigns the attack, control, and defence weights based on the given squad's
     * playing style. Each weight starts at the initial baseline value and is then
     * biased according to the squad's style.
     *
     * @param squad the squad whose {@link Style} determines weight adjustments
     */
    public void assignWeights(Squad squad)
    {
        this.attack = INITIAL_VALUE;
        this.control = INITIAL_VALUE;
        this.defence = INITIAL_VALUE;
        if(squad.getTeamStyle() == Style.ATTACKING)
        {
            attack += STYLE_BIAS;
            control -= STYLE_DEDUCTION;
            defence -= STYLE_DEDUCTION;
        }
        else if(squad.getTeamStyle() == Style.CONTROLLING)
        {
            control += STYLE_BIAS;
            attack -= STYLE_DEDUCTION;
            defence -= STYLE_DEDUCTION;
        }
        else if(squad.getTeamStyle() == Style.DEFENSIVE)
        {
            defence += STYLE_BIAS;
            attack -=  STYLE_DEDUCTION;
            control -= STYLE_DEDUCTION;
        }
    }

    /**
     * Returns the current attack weight.
     *
     * @return the attack weight value between 0.0 and 1.0
     */
    public double getAttack() {
        return this.attack;
    }

    /**
     * Returns the current control weight.
     *
     * @return the control weight value between 0.0 and 1.0
     */
    public double getControl() {
        return this.control;
    }

    /**
     * Returns the current defence weight.
     *
     * @return the defence weight value between 0.0 and 1.0
     */
    public double getDefence() {
        return this.defence;
    }

    /**
     * Returns a formatted string displaying the attack, defence, and control
     * weights, each truncated to two decimal places.
     *
     * @return a human-readable representation of the intent weights
     */
    @Override
    public String toString() {
        String attack = Double.toString(getAttack());
        String defence = Double.toString(getDefence());
        String control = Double.toString(getControl());
        attack = attack.substring(0, attack.indexOf('.') + 3);
        defence = defence.substring(0, defence.indexOf('.') + 3);
        control = control.substring(0, control.indexOf('.') + 3);
        return "+ Attack - " + attack + "\n+ Defence - " + defence + "\n+ Control - " + control;
    }
}
