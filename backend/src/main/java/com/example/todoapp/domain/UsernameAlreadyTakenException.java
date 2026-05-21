package com.example.todoapp.domain;

public class UsernameAlreadyTakenException extends RuntimeException {
    public static final String MESSAGE = "Username already taken.";

    public UsernameAlreadyTakenException() {
        super(MESSAGE);
    }
}
