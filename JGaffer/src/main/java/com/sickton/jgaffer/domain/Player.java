package com.sickton.jgaffer.domain;

import com.sickton.jgaffer.exceptions.*;

public class Player {

    protected static final int MAX_ADAPTABILITY = 5;
    protected static final int MIN_ADAPTABILITY = 0;

    private final String name;
    private final Position pos;
    private final int adaptabilityScore;

    public Player(String name, Position p, int as) {
        this.name = name;
        this.pos = p;
        if(as > MAX_ADAPTABILITY || as < MIN_ADAPTABILITY)
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
