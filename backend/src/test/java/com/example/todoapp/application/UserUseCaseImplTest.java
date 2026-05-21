package com.example.todoapp.application;

import com.example.todoapp.domain.InvalidCredentialsException;
import com.example.todoapp.domain.UsernameAlreadyTakenException;
import com.example.todoapp.domain.model.AuthenticatedUser;
import com.example.todoapp.domain.model.User;
import com.example.todoapp.domain.port.out.PasswordHasher;
import com.example.todoapp.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUseCaseImplTest {

    @Mock
    UserRepository repository;

    @Mock
    PasswordHasher passwordHasher;

    @InjectMocks
    UserUseCaseImpl userUseCase;

    @Test
    void register_savesUserWithHashedPassword() {
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(passwordHasher.hash("secret")).thenReturn("hashed-secret");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userUseCase.register("alice", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("hashed-secret", captor.getValue().getPasswordHash());
    }

    @Test
    void register_returnsUserWithCorrectUsernameAndId() {
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(passwordHasher.hash("secret")).thenReturn("hashed-secret");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userUseCase.register("alice", "secret");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        assertNotNull(result.getId());
    }

    @Test
    void register_whenUsernameAlreadyExists_throwsException() {
        when(repository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameAlreadyTakenException.class, () -> userUseCase.register("alice", "secret"));
        verify(repository, never()).save(any());
    }

    @Test
    void authenticate_withValidCredentials_returnsAuthenticatedUser() {
        User stored = new User(UUID.randomUUID(), "alice", "hashed-secret");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(stored));
        when(passwordHasher.matches("secret", "hashed-secret")).thenReturn(true);

        AuthenticatedUser result = userUseCase.authenticate("alice", "secret");

        assertNotNull(result);
        assertEquals(stored.getId(), result.getId());
        assertEquals("alice", result.getUsername());
    }

    @Test
    void authenticate_withWrongPassword_throwsInvalidCredentialsException() {
        User stored = new User(UUID.randomUUID(), "alice", "hashed-secret");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(stored));
        when(passwordHasher.matches("wrong", "hashed-secret")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userUseCase.authenticate("alice", "wrong"));
    }

    @Test
    void authenticate_withUnknownUsername_throwsInvalidCredentialsException() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userUseCase.authenticate("ghost", "secret"));
    }
}
