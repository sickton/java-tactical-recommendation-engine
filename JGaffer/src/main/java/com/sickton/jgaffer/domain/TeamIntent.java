package com.sickton.jgaffer.domain;

public class TeamIntent {
    protected static final double STYLE_BIAS = 0.1;
    protected static final double STYLE_DEDUCTION = 0.05;
    protected static final double INITIAL_VALUE = 0.33;

    private double attack;
    private double control;
    private double defence;

    public TeamIntent(Squad s) {
        assignWeights(s);
    }

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

    public double getAttack() {
        return this.attack;
    }

    public double getControl() {
        return this.control;
    }

    public double getDefence() {
        return this.defence;
    }
}
