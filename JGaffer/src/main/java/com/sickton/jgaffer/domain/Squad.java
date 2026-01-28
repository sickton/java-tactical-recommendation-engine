package com.sickton.jgaffer.domain;

import java.util.Map;

public class Squad {
    private final String team;
    private final Map<Player, Boolean> playerAvailability;
    private final String manager;
    private final Style teamStyle;

    public Squad(String t, Map<Player, Boolean> p, String m, Style ts) {
        this.team = t;
        this.playerAvailability = p;
        this.manager = m;
        this.teamStyle = ts;
    }

    public String getTeam() {
        return this.team;
    }

    public Map<Player, Boolean> getPlayerAvailability() {
        return this.playerAvailability;
    }

    public String getManager() {
        return this.manager;
    }

    public Style getTeamStyle() {
        return this.teamStyle;
    }
}
