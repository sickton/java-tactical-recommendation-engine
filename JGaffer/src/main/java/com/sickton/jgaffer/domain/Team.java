package com.sickton.jgaffer.domain;

import java.util.*;

public class Team {
    private final String name;
    private final Squad squad;
    private final Formation form;
    protected final TeamIntent intent;
    private final StaminaLevel staminaLevel;
    private final TeamAdaptability adaptability;

    public Team(Squad s, Formation form, StaminaLevel stamina, TeamAdaptability adaptability) {
        this.squad = s;
        this.name = s.getTeam();
        this.form = form;
        this.intent = new TeamIntent(s);
        this.staminaLevel = stamina;
        this.adaptability = adaptability;
    }

    public TeamIntent getIntent() {
        return intent;
    }
    
    public String getName() {
        return name;
    }

    public Squad getSquad() {
        return squad;
    }

    public StaminaLevel getStaminaLevel() {
        return staminaLevel;
    }

    public TeamAdaptability getAdaptabilityLevel() {
        return adaptability;
    }

    public Formation getForm() {
        return form;
    }
}
