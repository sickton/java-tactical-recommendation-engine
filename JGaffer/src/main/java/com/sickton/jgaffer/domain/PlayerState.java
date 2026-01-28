package com.sickton.jgaffer.domain;

public class PlayerState {
    private final Player player;
    private final InjuryStatus injury;
    private final StaminaLevel stamina;

    public PlayerState(Player player, InjuryStatus i, StaminaLevel s)
    {
        this.player = player;
        this.injury = i;
        this.stamina = s;
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
}
