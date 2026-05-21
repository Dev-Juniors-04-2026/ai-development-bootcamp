package com.example.todoapp.domain.model;

import java.util.UUID;

public class AuthenticatedUser {

    private final UUID id;
    private final String username;

    public AuthenticatedUser(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
}
