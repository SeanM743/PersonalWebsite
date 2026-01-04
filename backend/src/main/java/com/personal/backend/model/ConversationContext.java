package com.personal.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationContext {
    
    private String sessionId;
    private String username;
    private Role userRole;
    
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    
    private int totalTokenCount;
    private int messageCount;
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        messageCount = messages.size();
        lastAccessedAt = LocalDateTime.now();
        
        // Update token count if available
        if (message.getTokenCount() > 0) {
            totalTokenCount += message.getTokenCount();
        }
    }
    
    public void addUserMessage(String content) {
        addMessage(ChatMessage.userMessage(content));
    }
    
    public void addAssistantMessage(String content) {
        addMessage(ChatMessage.assistantMessage(content));
    }
    
    public void addAssistantMessage(String content, List<String> functionsUsed) {
        addMessage(ChatMessage.assistantMessage(content, functionsUsed));
    }
    
    public void addSystemMessage(String content) {
        addMessage(ChatMessage.systemMessage(content));
    }
    
    public List<ChatMessage> getRecentMessages(int count) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - count);
        return messages.subList(fromIndex, size);
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void updateExpiration(int timeoutMinutes) {
        expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
        lastAccessedAt = LocalDateTime.now();
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
}