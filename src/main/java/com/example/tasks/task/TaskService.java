package com.example.tasks.task;

import com.example.tasks.user.User;
import com.example.tasks.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

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

    public Task create(String title, String text) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Task task = new Task();
        task.setTitle(title.trim());
        task.setText(text);
        task.setCompleted(false);
        task.setUser(currentUser());
        return tasks.save(task);
    }

    public List<Task> findAll() {
        return tasks.findAll();
    }

    public List<Task> findByUser(long userId) {
        return tasks.findByUser_Id(userId);
    }

    public Task findById(long id) {
        Task task = tasks.findById(id).orElse(null);
        if (task == null) {
            throw new NoSuchElementException("Task not found");
        }
        return task;
    }

    private void checkOwner(Task task) {
        if (task.getUser().getId() != currentUser().getId()) {
            throw new NoSuchElementException("Task not found");
        }
    }

    public Task complete(long id) {
        Task task = findById(id);
        checkOwner(task);
        if (!task.isCompleted()) {
            task.setCompleted(true);
            tasks.save(task);
        }
        return task;
    }

    public void delete(long id) {
        Task task = findById(id);
        checkOwner(task);
        tasks.delete(task);
    }
}
