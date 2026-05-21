package com.example.todoapp.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;
    private String username;
    private String passwordHash;

    protected UserJpaEntity() {}

    public UserJpaEntity(UUID id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
