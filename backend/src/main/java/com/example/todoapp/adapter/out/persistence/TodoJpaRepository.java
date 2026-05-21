package com.example.todoapp.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    List<TodoJpaEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<TodoJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Transactional
    void deleteByIdAndUserId(UUID id, UUID userId);
}
