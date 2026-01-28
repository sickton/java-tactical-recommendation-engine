package com.sickton.jgaffer.domain;

public class MatchContext {
    private final String title;
    private final Team home;
    private final Team away;
    private final int h_goals;
    private final int a_goals;
    private final int minute;
    private final double h_pos;
    private final double a_pos;

    public MatchContext(String t, Team h, Team a, int hg, int ag, int m, double hp, double ap)
    {
        this.title = t;
        this.home = h;
        this.away = a;
        this.h_goals = hg;
        this.a_goals = ag;
        this.minute = m;
        this.h_pos = hp;
        this.a_pos = ap;
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

    public int getH_goals() {
        return h_goals;
    }

    public int getA_goals() {
        return a_goals;
    }

    public int getMinute() {
        return minute;
    }

    public double getH_pos() {
        return h_pos;
    }

    public double getA_pos() {
        return a_pos;
    }
}
