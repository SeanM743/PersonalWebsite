package com.personal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    
    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message must be less than 4000 characters")
    private String message;
    
    private String sessionId;
    
    private String authToken;  // Added for authentication
    
    private Map<String, Object> context;
    
    private boolean includeContext = true;
    
    private boolean enableFunctions = true;
}