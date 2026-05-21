package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.Todo;
import com.example.todoapp.domain.port.in.TodoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoUseCase todoUseCase;

    public TodoController(TodoUseCase todoUseCase) {
        this.todoUseCase = todoUseCase;
    }

    @GetMapping
    public List<TodoResponse> getAll(@AuthenticationPrincipal UUID userId) {
        return todoUseCase.getAll(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody CreateTodoRequest request) {
        return toResponse(todoUseCase.create(userId, request.title(), request.dueDate()));
    }

    @PatchMapping("/{id}")
    public TodoResponse toggle(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return toResponse(todoUseCase.toggle(userId, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        todoUseCase.delete(userId, id);
    }

    private TodoResponse toResponse(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getTitle(), todo.isCompleted(), todo.getDueDate());
    }
}
