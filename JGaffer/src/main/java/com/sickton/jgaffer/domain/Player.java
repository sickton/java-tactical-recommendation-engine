package com.sickton.jgaffer.domain;

public class Player {
    private final String name;
    private final Position pos;
    private final int adaptabilityScore;

    public Player(String name, Position p, int as) {
        this.name = name;
        this.pos = p;
        this.adaptabilityScore = as;
    }

    public String getName()
    {
        return this.name;
    }

    public Position getPos()
    {
        return this.pos;
    }

    public int getAdaptabilityScore()
    {
        return this.adaptabilityScore;
    }
}
