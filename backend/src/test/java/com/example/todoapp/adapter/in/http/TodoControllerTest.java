package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.Todo;
import com.example.todoapp.domain.port.in.TodoUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@AutoConfigureMockMvc(addFilters = false)
class TodoControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TodoUseCase todoUseCase;

    @BeforeEach
    void setUp() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllTodos_returns200WithTodos() throws Exception {
        when(todoUseCase.getAll(USER_ID)).thenReturn(List.of(
                new Todo(null, "Buy milk", false, null, USER_ID),
                new Todo(null, "Walk the dog", false, null, USER_ID)
        ));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Buy milk"))
                .andExpect(jsonPath("$[1].title").value("Walk the dog"));
    }

    @Test
    void postTodo_returns201WithTitleAndCompleted() throws Exception {
        when(todoUseCase.create(USER_ID, "Buy milk", null))
                .thenReturn(new Todo(null, "Buy milk", false, null, USER_ID));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Buy milk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.dueDate").isEmpty());
    }

    @Test
    void patchToggle_returns200WithUpdatedTodo() throws Exception {
        UUID id = UUID.randomUUID();
        boolean anyCompletedState = true;
        when(todoUseCase.toggle(USER_ID, id))
                .thenReturn(new Todo(id, "Buy milk", anyCompletedState, null, USER_ID));

        mockMvc.perform(patch("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(anyCompletedState))
                .andExpect(jsonPath("$.dueDate").isEmpty());
    }

    @Test
    void deleteTodo_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(todoUseCase).delete(USER_ID, id);

        mockMvc.perform(delete("/api/todos/{id}", id))
                .andExpect(status().isNoContent());

        verify(todoUseCase).delete(USER_ID, id);
    }

    @Test
    void postTodo_withDueDate_returns201WithDueDate() throws Exception {
        when(todoUseCase.create(USER_ID, "Submit report", "2026-05-10"))
                .thenReturn(new Todo(null, "Submit report", false, "2026-05-10", USER_ID));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Submit report", "dueDate": "2026-05-10"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Submit report"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-10"));
    }

}
