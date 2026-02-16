package com.sickton.jgaffer.domain;

import java.util.Map;

/**
 * Represents the identity of a football squad.
 *
 * <p>Contains the team name, manager name, and the team's fundamental playing {@link Style}.
 * This class serves as the foundational identity object that feeds into {@link Team}
 * and {@link TeamIntent} construction.</p>
 *
 * @author sickton
 * @see Team
 * @see Style
 */
public class Squad {
    private final String team;
    private final String manager;
    private final Style teamStyle;

    /**
     * Constructs a new Squad with a team name, manager name, and playing style.
     *
     * @param t  the team name
     * @param m  the manager name
     * @param ts the team's playing style
     */
    public Squad(String t, String m, Style ts) {
        this.team = t;
        this.manager = m;
        this.teamStyle = ts;
    }

    /**
     * Returns the team name.
     *
     * @return the team name
     */
    public String getTeam() {
        return this.team;
    }

    /**
     * Returns the manager name.
     *
     * @return the manager name
     */
    public String getManager() {
        return this.manager;
    }

    /**
     * Returns the team's playing style.
     *
     * @return the {@link Style} of the team
     */
    public Style getTeamStyle() {
        return this.teamStyle;
    }
}
