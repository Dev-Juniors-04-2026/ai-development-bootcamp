package com.example.todoapp.adapter.out.persistence;

import com.example.todoapp.domain.model.Todo;
import com.example.todoapp.domain.port.out.TodoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TodoPersistenceAdapter implements TodoRepository {

    private final TodoJpaRepository jpaRepository;

    public TodoPersistenceAdapter(TodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = toEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Todo> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Todo> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    private TodoJpaEntity toEntity(Todo todo) {
        return new TodoJpaEntity(todo.getId(), todo.getTitle(), todo.isCompleted(), todo.getDueDate(), todo.getUserId());
    }

    private Todo toDomain(TodoJpaEntity entity) {
        return new Todo(entity.getId(), entity.getTitle(), entity.isCompleted(), entity.getDueDate(), entity.getUserId());
    }
}
