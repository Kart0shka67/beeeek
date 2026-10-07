package com.example.tasks.task;

import com.example.tasks.user.User;
import com.example.tasks.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private TaskRepository tasks;
    private UserRepository users;

    public TaskService(TaskRepository tasks, UserRepository users) {
        this.tasks = tasks;
        this.users = users;
    }

    public Task create(String title, String text) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Task task = new Task();
        task.setTitle(title.trim());
        task.setText(text);
        task.setCompleted(false);
        task.setDeleted(false);
        return tasks.save(task);
    }

    // Create task and assign to multiple users (by their IDs)
    public Task createAndAssign(String title, String text, List<Long> assigneeIds) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Task task = new Task();
        task.setTitle(title.trim());
        task.setText(text);
        task.setCompleted(false);
        task.setDeleted(false);
        if (assigneeIds != null) {
            for (Long id : assigneeIds) {
                User u = users.findById(id).orElse(null);
                if (u != null) {
                    task.addAssignee(u);
                }
            }
        }
        return tasks.save(task);
    }

    // All active tasks
    public List<Task> findAllActive() {
        return tasks.findAllActive();
    }

    // All deleted tasks
    public List<Task> findAllDeleted() {
        return tasks.findAllDeleted();
    }

    // All tasks of a specific user
    public List<Task> findAllByUser(long userId) {
        return tasks.findByOwner(users.findById(userId).orElseThrow());
    }

    public Task findById(long id) {
        Task task = tasks.findById(id).orElse(null);
        if (task == null) {
            throw new NoSuchElementException("Task not found");
        }
        return task;
    }

    public Task complete(long id) {
        Task task = findById(id);
        task.setCompleted(true);
        return tasks.save(task);
    }

    public Task uncomplete(long id) {
        Task task = findById(id);
        task.setCompleted(false);
        return tasks.save(task);
    }

    // Soft delete: mark deleted=true
    public void delete(long id) {
        Task task = findById(id);
        task.setDeleted(true);
        tasks.save(task);
    }

    // Restore deleted task
    public Task restore(long id) {
        Task task = tasks.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found"));
        task.setDeleted(false);
        return tasks.save(task);
    }
}