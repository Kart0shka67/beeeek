package com.example.tasks.task;

import com.example.tasks.user.User;
import com.example.tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
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

    private Long currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) return null;
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private User currentUser() {
        Long id = currentUserId();
        if (id == null) return null;
        return users.findById(id).orElse(null);
    }

    private boolean isAdmin() {
        User u = currentUser();
        return u != null && u.getRole() == User.Role.ADMIN;
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
        User owner = currentUser();
        if (owner != null) task.setOwner(owner);
        return tasks.save(task);
    }

    public Task createAndAssign(String title, String text, List<Long> assigneeIds) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Task task = new Task();
        task.setTitle(title.trim());
        task.setText(text);
        task.setCompleted(false);
        task.setDeleted(false);
        User owner = currentUser();
        if (owner != null) task.setOwner(owner);
        if (assigneeIds != null) {
            for (Long id : assigneeIds) {
                User u = users.findById(id).orElse(null);
                if (u != null) task.addAssignee(u);
            }
        }
        return tasks.save(task);
    }

    // Regular user: own + assigned, active only
    public List<Task> findMyTasks() {
        User me = currentUser();
        if (me == null) return List.of();
        List<Task> result = new ArrayList<>();
        result.addAll(tasks.findActiveByOwnerId(me.getId()));
        result.addAll(tasks.findActiveAssignedTo(me.getId()));
        return result.stream().distinct().collect(Collectors.toList());
    }

    // Admin: all active
    public List<Task> findAllActive() {
        if (!isAdmin()) throw new NoSuchElementException("Access denied");
        return tasks.findAllActive();
    }

    // Admin: all deleted
    public List<Task> findAllDeleted() {
        if (!isAdmin()) throw new NoSuchElementException("Access denied");
        return tasks.findAllDeleted();
    }

    // Admin: all tasks of user
    public List<Task> findAllByUser(long userId) {
        if (!isAdmin()) throw new NoSuchElementException("Access denied");
        return tasks.findByOwner(users.findById(userId).orElseThrow());
    }

    public Task findById(long id) {
        Task task = tasks.findById(id).orElse(null);
        if (task == null) throw new NoSuchElementException("Task not found");

        // Admin sees everything
        if (isAdmin()) return task;

        // Regular user: only own/assigned, and not deleted
        User me = currentUser();
        if (me == null) throw new NoSuchElementException("Task not found");

        boolean isOwner = task.getOwner() != null && task.getOwner().getId() == currentUserId();
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId() == currentUserId());

        if (!isOwner && !isAssignee) throw new NoSuchElementException("Task not found");
        if (task.isDeleted()) throw new NoSuchElementException("Task not found");

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

    public void delete(long id) {
        Task task = findById(id);
        task.setDeleted(true);
        tasks.save(task);
    }

    public Task restore(long id) {
        if (!isAdmin()) throw new NoSuchElementException("Access denied");
        Task task = tasks.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found"));
        task.setDeleted(false);
        return tasks.save(task);
    }
}