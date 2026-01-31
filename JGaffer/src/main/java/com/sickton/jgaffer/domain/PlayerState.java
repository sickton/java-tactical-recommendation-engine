package com.sickton.jgaffer.domain;

public class PlayerState {
    private final Player player;
    private final InjuryStatus injury;
    private final StaminaLevel stamina;
    private final boolean redCarded;

    public PlayerState(Player player, InjuryStatus i, StaminaLevel s, boolean redCarded)
    {
        this.player = player;
        this.injury = i;
        this.stamina = s;
        this.redCarded = redCarded;
    }

    public Player getPlayer()
    {
        return this.player;
    }

    public InjuryStatus getInjury()
    {
        return this.injury;
    }

    public StaminaLevel getStamina()
    {
        return this.stamina;
    }

    public boolean isRedCarded()
    {
        return this.redCarded;
    }
}
