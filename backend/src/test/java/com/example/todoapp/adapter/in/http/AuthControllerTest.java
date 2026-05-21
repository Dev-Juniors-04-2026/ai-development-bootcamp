package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.AuthenticatedUser;
import com.example.todoapp.domain.model.User;
import com.example.todoapp.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserUseCase userUseCase;

    @MockitoBean
    JwtService jwtService;

    @Test
    void register_validBody_returns201WithToken() throws Exception {
        UUID id = UUID.randomUUID();
        AuthenticatedUser authenticated = new AuthenticatedUser(id, "alice");
        when(userUseCase.register("alice", "secret")).thenReturn(new User(id, "alice", "hash"));
        when(userUseCase.authenticate("alice", "secret")).thenReturn(authenticated);
        when(jwtService.issue(authenticated)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "secret"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        UUID id = UUID.randomUUID();
        AuthenticatedUser authenticated = new AuthenticatedUser(id, "carol");
        when(userUseCase.authenticate("carol", "pass456")).thenReturn(authenticated);
        when(jwtService.issue(authenticated)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "carol", "password": "pass456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

}
