package org.example.config;

import org.example.entity.User;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        logger.info("Checking for unhashed passwords in the database...");
        List<User> users = userRepository.findAll();
        boolean updated = false;

        for (User user : users) {
            String password = user.getPassword();
            // BCrypt passwords start with $2a$, $2b$, or $2y$ and are 60 chars long
            if (!isBCrypt(password)) {
                logger.info("Hashing password for user: {}", user.getUsername());
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                updated = true;
            }
        }

        if (updated) {
            logger.info("Database migration: All plain-text passwords have been hashed.");
        } else {
            logger.info("Database migration: All passwords are already hashed.");
        }
    }

    private boolean isBCrypt(String password) {
        if (password == null || password.length() != 60) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
