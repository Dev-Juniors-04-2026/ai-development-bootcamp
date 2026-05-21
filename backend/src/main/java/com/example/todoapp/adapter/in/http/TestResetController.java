package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.port.out.TodoRepository;
import com.example.todoapp.domain.port.out.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/api/todos")
public class TestResetController {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TestResetController(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @DeleteMapping("/reset")
    public void reset() {
        todoRepository.deleteAll();
        userRepository.deleteAll();
    }
}
