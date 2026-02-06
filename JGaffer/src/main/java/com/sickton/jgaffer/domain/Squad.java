package com.sickton.jgaffer.domain;

import java.util.Map;

public class Squad {
    private final String team;
    private final String manager;
    private final Style teamStyle;

    public Squad(String t, String m, Style ts) {
        this.team = t;
        this.manager = m;
        this.teamStyle = ts;
    }

    public String getTeam() {
        return this.team;
    }

    public String getManager() {
        return this.manager;
    }

    public Style getTeamStyle() {
        return this.teamStyle;
    }
}
