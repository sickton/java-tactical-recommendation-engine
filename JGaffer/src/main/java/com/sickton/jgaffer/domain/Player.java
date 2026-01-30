package com.sickton.jgaffer.domain;

import com.sickton.jgaffer.exceptions.*;

public class Player {
    private final String name;
    private final Position pos;
    private final int adaptabilityScore;

    public Player(String name, Position p, int as) {
        this.name = name;
        this.pos = p;
        if(as > 5 || as < 0)
                throw new PlayerException("Player has invalid adaptability score");
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
