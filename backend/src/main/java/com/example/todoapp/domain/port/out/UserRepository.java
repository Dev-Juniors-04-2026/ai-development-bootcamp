package com.example.todoapp.domain.port.out;

import com.example.todoapp.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteAll();
}
