package com.example.todoapp.adapter.out.persistence;

import com.example.todoapp.domain.model.User;
import com.example.todoapp.domain.port.out.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserPersistenceAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    private UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getId(), user.getUsername(), user.getPasswordHash());
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash());
    }
}
