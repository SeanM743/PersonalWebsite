package com.personal.backend.service;

import com.personal.backend.model.Role;
import com.personal.backend.model.User;
import com.personal.backend.repository.UserRepository;
import com.personal.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    public String authenticateUser(String username, String password) {
        try {
            log.debug("Attempting authentication for user: {}", username);
            
            // Validate credentials using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // Get user details
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                log.warn("User not found after successful authentication: {}", username);
                throw new BadCredentialsException("User not found");
            }
            
            User user = userOptional.get();
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            
            log.info("User {} authenticated successfully with role {}", username, user.getRole());
            return token;
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", username, e.getMessage());
            throw new BadCredentialsException("Invalid username or password");
        }
    }
    
    public boolean validateCredentials(String username, String password) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                log.debug("User not found: {}", username);
                return false;
            }
            
            User user = userOptional.get();
            boolean isValid = passwordEncoder.matches(password, user.getPasswordHash());
            
            log.debug("Credential validation for user {}: {}", username, isValid ? "success" : "failed");
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating credentials for user {}: {}", username, e.getMessage());
            return false;
        }
    }
    
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }
    
    public User createUser(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        String hashedPassword = hashPassword(password);
        
        User user = User.builder()
                .username(username)
                .passwordHash(hashedPassword)
                .role(role != null ? role : Role.GUEST)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created new user: {} with role: {}", username, savedUser.getRole());
        
        return savedUser;
    }
    
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public void updateUserPassword(String username, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        
        User user = userOptional.get();
        String hashedPassword = hashPassword(newPassword);
        user.setPasswordHash(hashedPassword);
        
        userRepository.save(user);
        log.info("Updated password for user: {}", username);
    }
    
    public String extractUsernameFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            return jwtUtil.extractUsername(token);
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }
    
    public Role getUserRole(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get().getRole();
        }
        
        log.warn("User not found when getting role: {}", username);
        return Role.GUEST; // Default to guest if user not found
    }
    
    public boolean validateToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}