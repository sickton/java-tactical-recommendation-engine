package com.sickton.jgaffer.exceptions;

public class PlayerException extends RuntimeException {
    public PlayerException(String message) {
        super(message);
    }

    public PlayerException()
    {
        this("Player creation error");
    }
}