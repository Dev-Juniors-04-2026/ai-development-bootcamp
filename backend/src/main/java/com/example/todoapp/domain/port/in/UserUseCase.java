package com.example.todoapp.domain.port.in;

import com.example.todoapp.domain.model.AuthenticatedUser;
import com.example.todoapp.domain.model.User;

public interface UserUseCase {

    User register(String username, String password);

    AuthenticatedUser authenticate(String username, String password);
}
