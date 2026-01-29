package com.sickton.jgaffer.domain;

public class MatchContext {
    private final String title;
    private final Team home;
    private final Team away;
    private final int homeGoals;
    private final int awayGoals;
    private final int minute;
    private final double homePossession;
    private final double awayPossession;

    public MatchContext(String t, Team h, Team a, int hg, int ag, int m, double hp, double ap)
    {
        this.title = t;
        this.home = h;
        this.away = a;
        this.homeGoals = hg;
        this.awayGoals = ag;
        this.minute = m;
        this.homePossession = hp;
        this.awayPossession = ap;
    }

    public String getTitle() {
        return title;
    }

    public Team getHome() {
        return home;
    }
    public Team getAway() {
        return away;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public int getMinute() {
        return minute;
    }

    public double getHomePossession() {
        return homePossession;
    }

    public double getAwayPossession() {
        return awayPossession;
    }
}
