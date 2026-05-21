package com.example.todoapp.domain.model;

import java.util.UUID;

public class Todo {

    private final UUID id;
    private final String title;
    private final boolean completed;
    private final String dueDate;
    private final UUID userId;

    public Todo(UUID id, String title, boolean completed, String dueDate, UUID userId) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.dueDate = dueDate;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }
    public String getDueDate() { return dueDate; }
    public UUID getUserId() { return userId; }
}
