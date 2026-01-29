package com.sickton.jgaffer.domain;

import java.util.*;

public class Team {
    private final String name;
    private final Squad squad;
    private final Set<Player> playingXI;
    private final Formation form;

    public Team(Squad s, Set<Player> playingXI, Formation form) {
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

    public Set<Player> getPlayingXI() {
        return playingXI;
    }

    public Formation getForm() {
        return form;
    }
}
