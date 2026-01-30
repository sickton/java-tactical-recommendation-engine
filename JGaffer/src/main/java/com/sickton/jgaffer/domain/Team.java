package com.sickton.jgaffer.domain;

import java.util.*;

public class Team {
    private final String name;
    private final Squad squad;
    private final Map<Player, PlayerState> playingXI;
    private final Formation form;

    public Team(Squad s, Map<Player, PlayerState> playingXI, Formation form) {
        this.squad = s;
        this.name = s.getTeam();
        this.playingXI = playingXI;
        this.form = form;
    }

    public String getName() {
        return name;
    }

    public Squad getSquad() {
        return squad;
    }

    public Map<Player, PlayerState> getPlayingXI() {
        return playingXI;
    }

    public Formation getForm() {
        return form;
    }
}
