package com.example.tasks.task;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "basic")
public class TaskController {

    private TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> getAll(
            @Parameter(description = "Show only tasks of this user. Without it all tasks are shown.")
            @RequestParam(required = false) Long userId) {
        if (userId == null) {
            return service.findAll();
        }
        return service.findByUser(userId);
    }

    @GetMapping("/{id}")
    public Task getOne(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody CreateTaskRequest body) {
        Task task = service.create(body.title(), body.text());
        URI location = URI.create("/api/tasks/" + task.getId());
        return ResponseEntity.created(location).body(task);
    }

    @PatchMapping("/{id}/complete")
    public Task complete(@PathVariable long id) {
        return service.complete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
