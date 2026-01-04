package com.personal.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    private String role; // "user", "assistant", "system"
    private String content;
    private LocalDateTime timestamp;
    private List<String> functionsUsed;
    private int tokenCount;
    
    public static ChatMessage userMessage(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatMessage assistantMessage(String content) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatMessage assistantMessage(String content, List<String> functionsUsed) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .functionsUsed(functionsUsed)
                .build();
    }
    
    public static ChatMessage systemMessage(String content) {
        return ChatMessage.builder()
                .role("system")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
}