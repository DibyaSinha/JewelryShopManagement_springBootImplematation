package org.example.service;

import org.example.exception.AuthenticationException;
import org.example.model.User;
import org.example.repository.impl.UserRepositoryImpl;
import org.example.repository.interfaces.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private UserRepository repo = new UserRepositoryImpl();

    public User login(String username, String password) {
        return repo.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .orElseThrow(() -> {
                    logger.warn("Failed login attempt for username: {}", username);
                    return new AuthenticationException("Invalid username or password");
                });
    }

    public void register(String username, String password, String role) {
        if (repo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User newUser = new User(0, username, password, role);
        repo.save(newUser);
        logger.info("Successfully registered new user: {} with role: {}", username, role);
    }
}
