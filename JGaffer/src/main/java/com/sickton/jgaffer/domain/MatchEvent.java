package com.sickton.jgaffer.domain;

/**
 * A single event during a match simulation (goal, phase change, or tactic note).
 */
public class MatchEvent {
    /** Event types used by the frontend to style each entry. */
    public enum Type {
        GOAL_HOME, GOAL_AWAY, PHASE_CHANGE, TACTIC_NOTE
    }

    private final int minute;
    private final Type type;
    private final String label;

    public MatchEvent(int minute, Type type, String label) {
        this.minute = minute;
        this.type = type;
        this.label = label;
    }

    public int getMinute() { return minute; }
    public Type getType()   { return type; }
    public String getLabel() { return label; }
}
