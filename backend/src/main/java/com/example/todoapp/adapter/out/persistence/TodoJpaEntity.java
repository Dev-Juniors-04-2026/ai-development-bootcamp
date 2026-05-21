package com.example.todoapp.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "todo")
public class TodoJpaEntity {

    @Id
    private UUID id;
    private String title;
    private boolean completed;
    private String dueDate;

    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected TodoJpaEntity() {}

    public TodoJpaEntity(UUID id, String title, boolean completed, String dueDate, UUID userId) {
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
