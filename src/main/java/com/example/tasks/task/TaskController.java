package com.example.tasks.task;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // My tasks (own + assigned, active only)
    @GetMapping
    public List<Task> getMyTasks() {
        return service.findMyTasks();
    }

    // All active tasks (admin only)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Task> getAllActive() {
        return service.findAllActive();
    }

    // All deleted tasks (admin only)
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Task> getAllDeleted() {
        return service.findAllDeleted();
    }

    // All tasks of a specific user (admin only)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Task> getUserTasks(@PathVariable long userId) {
        return service.findAllByUser(userId);
    }

    @GetMapping("/{id}")
    public Task getOne(@PathVariable long id) {
        return service.findById(id);
    }

    // Create task for yourself (backward compatible)
    @PostMapping
    public ResponseEntity<Task> create(@RequestBody CreateTaskRequest body) {
        Task task = service.create(body.title(), body.text());
        URI location = URI.create("/api/tasks/" + task.getId());
        return ResponseEntity.created(location).body(task);
    }

    // Create task and assign to multiple users
    @PostMapping("/assign")
    public ResponseEntity<Task> createAndAssign(@RequestBody AssignTaskRequest body) {
        Task task = service.createAndAssign(body.title(), body.text(), body.assigneeIds());
        URI location = URI.create("/api/tasks/" + task.getId());
        return ResponseEntity.created(location).body(task);
    }

    @PatchMapping("/{id}/complete")
    public Task complete(@PathVariable long id) {
        return service.complete(id);
    }

    @PatchMapping("/{id}/uncomplete")
    public Task uncomplete(@PathVariable long id) {
        return service.uncomplete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    // Admin: restore soft-deleted task
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public Task restore(@PathVariable long id) {
        return service.restore(id);
    }
}