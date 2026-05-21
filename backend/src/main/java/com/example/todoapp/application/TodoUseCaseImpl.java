package com.example.todoapp.application;

import com.example.todoapp.domain.model.Todo;
import com.example.todoapp.domain.port.in.TodoUseCase;
import com.example.todoapp.domain.port.out.TodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TodoUseCaseImpl implements TodoUseCase {

    private final TodoRepository repository;

    public TodoUseCaseImpl(TodoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Todo> getAll(UUID userId) {
        return repository.findAllByUserId(userId);
    }

    @Override
    public Todo create(UUID userId, String title, String dueDate) {
        return repository.save(new Todo(UUID.randomUUID(), title, false, dueDate, userId));
    }

    @Override
    public Todo toggle(UUID userId, UUID id) {
        Todo todo = repository.findByIdAndUserId(id, userId).orElseThrow();
        return repository.save(new Todo(todo.getId(), todo.getTitle(), !todo.isCompleted(), todo.getDueDate(), todo.getUserId()));
    }

    @Override
    public void delete(UUID userId, UUID id) {
        repository.findByIdAndUserId(id, userId).orElseThrow();
        repository.deleteByIdAndUserId(id, userId);
    }
}
