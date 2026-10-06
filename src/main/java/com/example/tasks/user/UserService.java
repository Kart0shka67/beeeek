package com.example.tasks.user;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private UserRepository users;
    private PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
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
        user.setPassword(encoder.encode(password));
        return users.save(user);
    }

    public User login(String username, String password) {
        User user = null;
        if (username != null) {
            user = users.findByUsername(username.trim());
        }
        if (user == null || password == null || !encoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        // Full name below is Spring Security's own User, not our entity.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
