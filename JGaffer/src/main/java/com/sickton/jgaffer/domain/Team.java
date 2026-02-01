package com.sickton.jgaffer.domain;

import com.sickton.jgaffer.exceptions.MatchTeamException;

import java.util.*;

public class Team {
    private final String name;
    private final Squad squad;
    private final Map<Player, PlayerState> playingXI;
    private final Formation form;
    private TeamIntent intent;

    public Team(Squad s, Map<Player, PlayerState> playingXI, Formation form) {
        this.squad = s;
        this.name = s.getTeam();
        this.playingXI = playingXI;
        this.form = form;
        this.intent = new TeamIntent(s);
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

    public Map<Player, PlayerState> getPlayingXI() {
        return playingXI;
    }

    public Formation getForm() {
        return form;
    }
}
