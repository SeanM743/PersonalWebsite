package com.personal.backend.dto;

import com.personal.backend.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private String username;
    private Role role;
    private String message;
    
    public static AuthResponse success(String token, String username, Role role) {
        return AuthResponse.builder()
                .token(token)
                .username(username)
                .role(role)
                .message("Authentication successful")
                .build();
    }
    
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .message(message)
                .build();
    }
}