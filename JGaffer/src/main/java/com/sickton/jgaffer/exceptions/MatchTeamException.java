package com.sickton.jgaffer.exceptions;

public class MatchTeamException extends RuntimeException {
    public MatchTeamException(String message) {
        super(message);
    }

    public MatchTeamException() {
        super("Match team error");
    }
}
