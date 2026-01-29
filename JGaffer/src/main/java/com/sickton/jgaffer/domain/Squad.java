package com.sickton.jgaffer.domain;

import java.util.Map;

public class Squad {
    private final String team;
    private final Map<Player, PlayerAvailability> playerAvailability;
    private final String manager;
    private final Style teamStyle;

    public Squad(String t, Map<Player, PlayerAvailability> p, String m, Style ts) {
        this.team = t;
        this.playerAvailability = p;
        this.manager = m;
        this.teamStyle = ts;
    }

    public String getTeam() {
        return this.team;
    }

    public Map<Player, PlayerAvailability> getPlayerAvailability() {
        return this.playerAvailability;
    }

    public String getManager() {
        return this.manager;
    }

    public Style getTeamStyle() {
        return this.teamStyle;
    }
}
