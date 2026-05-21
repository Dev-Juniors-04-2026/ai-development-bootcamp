package com.example.todoapp.application;

import com.example.todoapp.domain.model.Todo;
import com.example.todoapp.domain.port.out.TodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoUseCaseImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    TodoRepository repository;

    @InjectMocks
    TodoUseCaseImpl todoUseCase;

    @Test
    void getAll_returnsAllTodosForUser() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(repository.findAllByUserId(USER_ID)).thenReturn(List.of(
                new Todo(id1, "Buy milk", false, null, USER_ID),
                new Todo(id2, "Walk the dog", true, null, USER_ID)
        ));

        List<Todo> result = todoUseCase.getAll(USER_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(id1, result.get(0).getId());
        assertEquals("Buy milk", result.get(0).getTitle());
        assertFalse(result.get(0).isCompleted());
        assertEquals(id2, result.get(1).getId());
        assertEquals("Walk the dog", result.get(1).getTitle());
        assertTrue(result.get(1).isCompleted());
    }

    @Test
    void create_savesAndReturnsTodoScopedToUser() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Todo result = todoUseCase.create(USER_ID, "Buy milk", null);

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(repository).save(captor.capture());

        assertNotNull(captor.getValue());
        assertEquals("Buy milk", captor.getValue().getTitle());
        assertFalse(captor.getValue().isCompleted());
        assertNotNull(captor.getValue().getId());
        assertEquals(USER_ID, captor.getValue().getUserId());

        assertNotNull(result);
        assertEquals("Buy milk", result.getTitle());
        assertFalse(result.isCompleted());
        assertNotNull(result.getId());
        assertEquals(USER_ID, result.getUserId());
    }

    @Test
    void toggle_whenTodoIsNotCompleted_returnsCompletedTodo() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(new Todo(id, "Buy milk", false, null, USER_ID)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Todo result = todoUseCase.toggle(USER_ID, id);

        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertEquals(id, result.getId());
        assertEquals("Buy milk", result.getTitle());
    }

    @Test
    void toggle_whenTodoIsCompleted_returnsNotCompletedTodo() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(new Todo(id, "Buy milk", true, null, USER_ID)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Todo result = todoUseCase.toggle(USER_ID, id);

        assertNotNull(result);
        assertFalse(result.isCompleted());
        assertEquals(id, result.getId());
        assertEquals("Buy milk", result.getTitle());
    }

    @Test
    void toggle_whenTodoDoesNotExist_throwsNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> todoUseCase.toggle(USER_ID, id));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_callsDeleteByIdAndUserId() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(new Todo(id, "Buy milk", false, null, USER_ID)));

        todoUseCase.delete(USER_ID, id);

        verify(repository).deleteByIdAndUserId(id, USER_ID);
    }

    @Test
    void delete_whenTodoDoesNotExist_throwsNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> todoUseCase.delete(USER_ID, id));
        verify(repository, never()).deleteByIdAndUserId(any(), any());
    }
}
