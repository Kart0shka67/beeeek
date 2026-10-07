package com.example.tasks.user;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (users.findByUsername(username.trim()) != null) {
            throw new IllegalArgumentException("Username is taken");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(User.Role.USER);
        return users.save(user);
    }

    public User login(String username, String password) {
        User user = null;
        if (username != null) {
            user = users.findByUsername(username.trim());
        }
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new NoSuchElementException("Invalid username or password");
        }
        return user;
    }

    public List<User> findAll() {
        return users.findAll();
    }
}