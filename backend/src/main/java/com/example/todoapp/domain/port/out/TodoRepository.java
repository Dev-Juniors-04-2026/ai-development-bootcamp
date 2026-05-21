package com.example.todoapp.domain.port.out;

import com.example.todoapp.domain.model.Todo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository {
    Todo save(Todo todo);
    List<Todo> findAllByUserId(UUID userId);
    Optional<Todo> findByIdAndUserId(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
    void deleteAll();
}
