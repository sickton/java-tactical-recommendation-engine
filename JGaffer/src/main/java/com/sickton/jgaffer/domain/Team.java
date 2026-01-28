package com.sickton.jgaffer.domain;

import java.util.List;

public class Team {
    private final String name;
    private final Squad squad;
    private final List<Player> playingXI;
    private final Formation form;

    public Team(Squad s, List<Player> playingXI, Formation form) {
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

    public List<Player> getPlayingXI() {
        return playingXI;
    }

    public Formation getForm() {
        return form;
    }
}
