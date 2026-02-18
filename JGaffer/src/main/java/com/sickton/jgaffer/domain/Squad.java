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
    private final Formation baseFormation;

    /**
     * Constructs a new Squad with a team name, manager name, playing style, and base formation.
     *
     * @param t  the team name
     * @param m  the manager name
     * @param ts the team's playing style
     * @param f  the team's base formation
     */
    public Squad(String t, String m, Style ts, Formation f) {
        this.team = t;
        this.manager = m;
        this.teamStyle = ts;
        this.baseFormation = f;
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

    /**
     * Returns the team's base formation.
     *
     * @return the {@link Formation} the team typically plays
     */
    public Formation getBaseFormation() {
        return this.baseFormation;
    }
}
