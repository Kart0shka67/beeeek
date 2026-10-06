package com.example.tasks.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest body) {
        User user = service.register(body.username(), body.password());
        Map<String, Object> answer = Map.of("id", user.getId(), "username", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(answer);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody RegisterRequest body) {
        User user = service.login(body.username(), body.password());
        return Map.of("id", user.getId(), "username", user.getUsername());
    }
}
