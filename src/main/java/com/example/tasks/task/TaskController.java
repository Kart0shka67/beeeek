package com.example.tasks.task;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class TaskController {

    private TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // All active tasks (was /api/tasks, now shows all active tasks)
    @GetMapping
    public List<Task> getAllTasks() {
        return service.findAllActive();
    }

    // All active tasks
    @GetMapping("/all")
    public List<Task> getAllActive() {
        return service.findAllActive();
    }

    // All deleted tasks
    @GetMapping("/deleted")
    public List<Task> getAllDeleted() {
        return service.findAllDeleted();
    }

    // All tasks of a specific user
    @GetMapping("/user/{userId}")
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
    public Task restore(@PathVariable long id) {
        return service.restore(id);
    }
}