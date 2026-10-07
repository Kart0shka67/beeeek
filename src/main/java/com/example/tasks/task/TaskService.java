package com.example.tasks.task;

import com.example.tasks.user.User;
import com.example.tasks.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private User currentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByUsername(name);
    }

    private boolean isAdmin() {
        return currentUser().isAdmin();
    }

    public Task create(String title, String text) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Task task = new Task();
        task.setTitle(title.trim());
        task.setText(text);
        task.setCompleted(false);
        task.setOwner(currentUser());
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
        task.setOwner(currentUser());
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

    // For regular user: own active tasks + assigned active tasks
    public List<Task> findMyTasks() {
        User me = currentUser();
        List<Task> result = new ArrayList<>();
        result.addAll(tasks.findActiveByOwnerId(me.getId()));
        result.addAll(tasks.findActiveAssignedTo(me.getId()));
        // Remove duplicates
        return result.stream().distinct().collect(Collectors.toList());
    }

    // Admin: all active tasks
    public List<Task> findAllActive() {
        if (!isAdmin()) {
            throw new NoSuchElementException("Access denied");
        }
        return tasks.findAllActive();
    }

    // Admin: all tasks of a specific user (including deleted)
    public List<Task> findAllByUser(long userId) {
        if (!isAdmin()) {
            throw new NoSuchElementException("Access denied");
        }
        return tasks.findByOwner(users.findById(userId).orElseThrow());
    }

    // Admin: all deleted tasks
    public List<Task> findAllDeleted() {
        if (!isAdmin()) {
            throw new NoSuchElementException("Access denied");
        }
        return tasks.findAllDeleted();
    }

    public Task findById(long id) {
        Task task = tasks.findById(id).orElse(null);
        if (task == null) {
            throw new NoSuchElementException("Task not found");
        }
        // Regular user can see own tasks and assigned tasks (even if soft-deleted? no, only active)
        // Admin can see everything
        if (!isAdmin()) {
            User me = currentUser();
            boolean isOwner = task.getOwner() != null && task.getOwner().getId() == me.getId();
            boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId() == me.getId());
            if (!isOwner && !isAssignee) {
                throw new NoSuchElementException("Task not found");
            }
            if (task.isDeleted()) {
                throw new NoSuchElementException("Task not found");
            }
        }
        return task;
    }

    public Task complete(long id) {
        Task task = findById(id);
        checkCanModify(task);
        if (!task.isCompleted()) {
            task.setCompleted(true);
            tasks.save(task);
        }
        return task;
    }

    public Task uncomplete(long id) {
        Task task = findById(id);
        checkCanModify(task);
        if (task.isCompleted()) {
            task.setCompleted(false);
            tasks.save(task);
        }
        return task;
    }

    // Soft delete: mark deleted=true, only owner or admin
    public void delete(long id) {
        Task task = findById(id);
        checkCanModify(task);
        task.setDeleted(true);
        tasks.save(task);
    }

    // Admin: restore deleted task
    public Task restore(long id) {
        if (!isAdmin()) {
            throw new NoSuchElementException("Access denied");
        }
        Task task = tasks.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found"));
        task.setDeleted(false);
        return tasks.save(task);
    }

    private void checkCanModify(Task task) {
        User me = currentUser();
        if (isAdmin()) return;
        if (task.getOwner() == null || task.getOwner().getId() != me.getId()) {
            throw new NoSuchElementException("Task not found");
        }
    }
}
