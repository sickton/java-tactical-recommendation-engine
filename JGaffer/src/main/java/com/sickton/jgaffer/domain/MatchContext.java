package com.sickton.jgaffer.domain;

/**
 * Immutable snapshot of a football match at a specific moment in time.
 *
 * <p>Captures the complete match state required for tactical evaluation, including
 * the participating teams, current scoreline, and the minute of play. This object
 * is passed to the {@link com.sickton.jgaffer.engine.TacticalRecommendationEngine}
 * as the primary input for generating a tactic recommendation.</p>
 *
 * @author sickton
 * @see Team
 * @see com.sickton.jgaffer.engine.TacticalRecommendationEngine
 */
public class MatchContext {
    private final String title;
    private final Team home;
    private final Team away;
    private final int homeGoals;
    private final int awayGoals;
    private final int minute;

    /**
     * Constructs a new MatchContext capturing the full state of a match at a given moment.
     *
     * @param t  the match title
     * @param h  the home team
     * @param a  the away team
     * @param hg the number of home goals scored
     * @param ag the number of away goals scored
     * @param m  the current minute of the match
     */
    public MatchContext(String t, Team h, Team a, int hg, int ag, int m)
    {
        this.title = t;
        this.home = h;
        this.away = a;
        this.homeGoals = hg;
        this.awayGoals = ag;
        this.minute = m;
    }

    /**
     * Returns the title of the match.
     *
     * @return the match title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the home team.
     *
     * @return the home {@link Team}
     */
    public Team getHome() {
        return home;
    }
    /**
     * Returns the away team.
     *
     * @return the away {@link Team}
     */
    public Team getAway() {
        return away;
    }

    /**
     * Returns the number of goals scored by the home team.
     *
     * @return the home team's goal count
     */
    public int getHomeGoals() {
        return homeGoals;
    }

    /**
     * Returns the number of goals scored by the away team.
     *
     * @return the away team's goal count
     */
    public int getAwayGoals() {
        return awayGoals;
    }

    /**
     * Returns the current minute of the match.
     *
     * @return the match minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Returns a formatted string representation of the match context, including
     * the title, minute, scoreline, team names, stamina levels, adaptability levels,
     * and team intents.
     *
     * @return a human-readable summary of the match context
     */
    @Override
    public String toString() {
        return "Match - " +
                getTitle() +
                "\n" +
                "Minutes - " +
                getMinute() + "'" +
                "\n" +
                "Scoreline - " +
                getHomeGoals() +
                "-" +
                getAwayGoals() +
                "\n" +
                "Home Team Name - " + getHome().getName() +
                "\n" +
                "Away Team Name - " + getAway().getName() +
                "\n" +
                "Team Stamina - " +
                getHome().getStaminaLevel() + " - " + getAway().getStaminaLevel() +
                "\n" +
                "Team Adaptability - " +
                getHome().getAdaptabilityLevel() + " - " + getAway().getAdaptabilityLevel() +
                "\n" +
                "Home Team Intent - " +
                getHome().getIntent().toString() + "\n Away Team Intent - " +  getAway().getIntent().toString() +
                "\n";
    }
}