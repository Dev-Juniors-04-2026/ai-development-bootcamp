package com.example.todoapp.domain.model;

import java.util.UUID;

public class User {

    private final UUID id;
    private final String username;
    private final String passwordHash;

    public User(UUID id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
