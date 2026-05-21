package com.example.todoapp.application;

import com.example.todoapp.domain.InvalidCredentialsException;
import com.example.todoapp.domain.UsernameAlreadyTakenException;
import com.example.todoapp.domain.model.AuthenticatedUser;
import com.example.todoapp.domain.model.User;
import com.example.todoapp.domain.port.in.UserUseCase;
import com.example.todoapp.domain.port.out.PasswordHasher;
import com.example.todoapp.domain.port.out.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserUseCaseImpl implements UserUseCase {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private String dummyHash;

    public UserUseCaseImpl(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    void initDummyHash() {
        this.dummyHash = passwordHasher.hash("dummy");
    }

    @Override
    public User register(String username, String password) {
        String normalized = normalize(username);
        if (repository.existsByUsername(normalized)) {
            throw new UsernameAlreadyTakenException();
        }
        return repository.save(new User(UUID.randomUUID(), normalized, passwordHasher.hash(password)));
    }

    @Override
    public AuthenticatedUser authenticate(String username, String password) {
        Optional<User> userOpt = repository.findByUsername(normalize(username));
        // Always run Argon2 verify (against dummy on miss) so response time doesn't leak username existence.
        String hashToVerify = userOpt.map(User::getPasswordHash).orElse(dummyHash);
        boolean ok = passwordHasher.matches(password, hashToVerify);
        if (!ok || userOpt.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        User user = userOpt.get();
        return new AuthenticatedUser(user.getId(), user.getUsername());
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
