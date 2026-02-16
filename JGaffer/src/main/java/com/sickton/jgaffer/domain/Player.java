package com.sickton.jgaffer.domain;

/**
 * Represents an individual football player with a name and field position.
 *
 * @author sickton
 * @see Position
 */
public class Player {
    private final String name;
    private final Position pos;

    /**
     * Constructs a new Player with the given name and field position.
     *
     * @param name the player name
     * @param p    the player's field position
     */
    public Player(String name, Position p) {
        this.name = name;
        this.pos = p;
    }

    /**
     * Returns the name of the player.
     *
     * @return the player name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Returns the field position of the player.
     *
     * @return the {@link Position} of the player
     */
    public Position getPos()
    {
        return this.pos;
    }
}
