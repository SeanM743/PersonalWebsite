package com.personal.backend.controller;

import com.personal.backend.dto.AuthResponse;
import com.personal.backend.dto.LoginRequest;
import com.personal.backend.model.User;
import com.personal.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                            BindingResult bindingResult) {
        
        // Handle validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                    .orElse("Validation failed");
            
            log.warn("Login validation failed: {}", errorMessage);
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Invalid request: " + errorMessage));
        }
        
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());
            
            // Authenticate user and get JWT token
            String token = authenticationService.authenticateUser(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
            );
            
            // Get user details for response
            Optional<User> userOptional = authenticationService.findUserByUsername(loginRequest.getUsername());
            if (userOptional.isEmpty()) {
                log.error("User not found after successful authentication: {}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(AuthResponse.error("Authentication error"));
            }
            
            User user = userOptional.get();
            AuthResponse response = AuthResponse.success(token, user.getUsername(), user.getRole());
            
            log.info("User {} logged in successfully with role {}", user.getUsername(), user.getRole());
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid username or password"));
            
        } catch (Exception e) {
            log.error("Unexpected error during login for user {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Authentication service temporarily unavailable"));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            // Token validation will be handled by JWT filter
            // This endpoint is mainly for frontend to check token validity
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Token is valid")
                    .build());
            
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid token"));
        }
    }
}