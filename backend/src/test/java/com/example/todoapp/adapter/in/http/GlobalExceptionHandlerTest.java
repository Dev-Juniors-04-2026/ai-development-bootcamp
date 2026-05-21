package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.InvalidCredentialsException;
import com.example.todoapp.domain.UsernameAlreadyTakenException;
import com.example.todoapp.domain.port.in.TodoUseCase;
import com.example.todoapp.domain.port.in.UserUseCase;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TodoController.class, AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TodoUseCase todoUseCase;

    @MockitoBean
    UserUseCase userUseCase;

    @MockitoBean
    JwtService jwtService;

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
    void usernameAlreadyTaken_returns409WithMessage() throws Exception {
        when(userUseCase.register("bob", "anything")).thenThrow(new UsernameAlreadyTakenException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "bob", "password": "anything"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(UsernameAlreadyTakenException.MESSAGE));
    }

    @Test
    void invalidCredentials_returns401WithMessage() throws Exception {
        when(userUseCase.authenticate("dave", "wrong")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "dave", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(InvalidCredentialsException.MESSAGE));
    }

    @Test
    void blankUsername_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "password": "secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("username: " + RegisterRequest.BLANK_MESSAGE));
    }

    @Test
    void blankPassword_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("password: " + RegisterRequest.BLANK_MESSAGE));
    }

    @Test
    void blankUsernameOnLogin_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "password": "secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("username: " + LoginRequest.BLANK_MESSAGE));
    }

    @Test
    void blankPasswordOnLogin_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("password: " + LoginRequest.BLANK_MESSAGE));
    }

    @Test
    void noSuchElementException_returns404WithMessage() throws Exception {
        when(todoUseCase.getAll(any(UUID.class))).thenThrow(new NoSuchElementException("Todo not found"));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Todo not found"));
    }



    @Test
    void missingTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("title: " + CreateTodoRequest.TITLE_BLANK_MESSAGE));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        when(todoUseCase.getAll(any(UUID.class))).thenThrow(new RuntimeException("internal details must not leak"));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.UNEXPECTED_ERROR_MESSAGE));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.MALFORMED_JSON_MESSAGE));
    }

    @Test
    void whitespaceOnlyTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("title: " + CreateTodoRequest.TITLE_BLANK_MESSAGE));
    }
    @Test
    void invalidDueDate_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Buy milk", "dueDate": "not-a-date"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dueDate: " + CreateTodoRequest.DUE_DATE_FORMAT_MESSAGE));
    }

    @Test
    void invalidDueDate_invalidMonth_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Buy milk", "dueDate": "2026-13-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dueDate: " + CreateTodoRequest.DUE_DATE_FORMAT_MESSAGE));
    }

    @Test
    void invalidDueDate_invalidDay_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Buy milk", "dueDate": "2026-02-30"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dueDate: " + CreateTodoRequest.DUE_DATE_FORMAT_MESSAGE));
    }

    @Test
    void invalidUuidInPath_returns400() throws Exception {
        mockMvc.perform(patch("/api/todos/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("id: invalid value 'not-a-uuid'"));
    }

    @Test
    void deleteTodo_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NoSuchElementException("Todo not found")).when(todoUseCase).delete(any(UUID.class), eq(id));

        mockMvc.perform(delete("/api/todos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Todo not found"));
    }

    @Test
    void deleteTodo_invalidUuid_returns400() throws Exception {
        mockMvc.perform(delete("/api/todos/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("id: invalid value 'not-a-uuid'"));
    }

    @Test
    void emptyTitle_returns400WithExactDetail() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("title: " + CreateTodoRequest.TITLE_BLANK_MESSAGE));
    }
}
