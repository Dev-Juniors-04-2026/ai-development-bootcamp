package com.example.todoapp.adapter.in.http;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = BLANK_MESSAGE) String username,
        @NotBlank(message = BLANK_MESSAGE) String password) {

    static final String BLANK_MESSAGE = "must not be blank";
}
