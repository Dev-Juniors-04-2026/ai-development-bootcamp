package com.example.todoapp.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void hash_returnsNonNullNonEmptyString() {
        String result = hasher.hash("secret");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void hash_doesNotReturnRawPassword() {
        String result = hasher.hash("secret");

        assertNotEquals("secret", result);
    }

    @Test
    void hash_producesDifferentHashesForSamePassword() {
        String first = hasher.hash("secret");
        String second = hasher.hash("secret");

        assertNotEquals(first, second);
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hash = hasher.hash("secret");

        assertTrue(hasher.matches("secret", hash));
    }

    @Test
    void matches_returnsFalseForIncorrectPassword() {
        String hash = hasher.hash("secret");

        assertFalse(hasher.matches("wrong", hash));
    }

}
