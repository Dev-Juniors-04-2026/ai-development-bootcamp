package com.example.todoapp.domain.port.in;

import com.example.todoapp.domain.model.Todo;

import java.util.List;
import java.util.UUID;

public interface TodoUseCase {
    List<Todo> getAll(UUID userId);
    Todo create(UUID userId, String title, String dueDate);
    Todo toggle(UUID userId, UUID id);
    void delete(UUID userId, UUID id);
}
