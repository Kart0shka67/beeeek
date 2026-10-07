package com.example.tasks.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest body) {
        User user = service.register(body.getUsername(), body.getPassword());
        Map<String, Object> answer = Map.of("id", user.getId(), "username", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(answer);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody RegisterRequest body) {
        User user = service.login(body.getUsername(), body.getPassword());
        return Map.of("id", user.getId(), "username", user.getUsername());
    }

    // List all registered users (for task assignment)
    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        return service.findAll().stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
