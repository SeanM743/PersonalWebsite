package com.personal.backend.dto;

import com.personal.backend.model.ChatMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    private String message;  // Changed from 'response' to 'message' for consistency
    private String sessionId;
    private LocalDateTime timestamp;
    private Map<String, Object> context;
    private List<String> functionsUsed;
    private List<String> availableFunctions;  // Added for function discovery
    private List<ChatMessage> conversationHistory;  // Added for history retrieval
    private String error;
    private boolean success;
    
    // Keep backward compatibility
    public String getResponse() {
        return message;
    }
    
    public void setResponse(String response) {
        this.message = response;
    }
    
    public static ChatResponse success(String message, String sessionId) {
        return ChatResponse.builder()
                .message(message)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }
    
    public static ChatResponse success(String message, String sessionId, List<String> functionsUsed) {
        return ChatResponse.builder()
                .message(message)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .functionsUsed(functionsUsed)
                .success(true)
                .build();
    }
    
    public static ChatResponse error(String error) {
        return ChatResponse.builder()
                .error(error)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }
    
    public static ChatResponse error(String error, String sessionId) {
        return ChatResponse.builder()
                .error(error)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }
}