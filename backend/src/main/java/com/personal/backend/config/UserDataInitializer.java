package com.personal.backend.config;

import com.personal.backend.model.Role;
import com.personal.backend.model.User;
import com.personal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with default users if they don't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeUsers();
    }

    private void initializeUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already initialized. Skipping.");
            return;
        }

        log.info("Initializing default users...");

        // Create Admin User
        // Username: admin
        // Password: default123
        createAdminUser();

        log.info("User initialization completed.");
    }

    private void createAdminUser() {
        String username = "admin";
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User admin = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("default123"))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Created default admin user: {}", username);
    }
}
