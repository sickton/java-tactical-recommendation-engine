package com.sickton.jgaffer.domain;

public class Player {
    private final String name;
    private final Position pos;

    public Player(String name, Position p) {
        this.name = name;
        this.pos = p;
    }

    public String getName()
    {
        return this.name;
    }

    public Position getPos()
    {
        return this.pos;
    }
}
