package com.personal.backend.service;

import com.personal.backend.model.ChatMessage;
import com.personal.backend.model.ConversationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContextTruncationService {
    
    private final GeminiChatClient geminiChatClient;
    
    @Value("${chat.context.max.messages:50}")
    private int maxContextMessages;
    
    @Value("${chat.context.max.tokens:8000}")
    private int maxContextTokens;
    
    public List<ChatMessage> truncateContext(ConversationContext context) {
        List<ChatMessage> messages = context.getMessages();
        
        if (messages.size() <= maxContextMessages && context.getTotalTokenCount() <= maxContextTokens) {
            return new ArrayList<>(messages);
        }
        
        log.debug("Truncating context for session: {} ({} messages, {} tokens)", 
                 context.getSessionId(), messages.size(), context.getTotalTokenCount());
        
        // Strategy 1: Keep recent messages within limits
        List<ChatMessage> truncatedMessages = truncateByRecentMessages(messages);
        
        // Strategy 2: If still too many tokens, truncate by token count
        if (estimateTotalTokens(truncatedMessages) > maxContextTokens) {
            truncatedMessages = truncateByTokenCount(truncatedMessages);
        }
        
        log.debug("Context truncated to {} messages with ~{} tokens", 
                 truncatedMessages.size(), estimateTotalTokens(truncatedMessages));
        
        return truncatedMessages;
    }
    
    private List<ChatMessage> truncateByRecentMessages(List<ChatMessage> messages) {
        if (messages.size() <= maxContextMessages) {
            return new ArrayList<>(messages);
        }
        
        // Keep the most recent messages, but preserve important system messages
        List<ChatMessage> truncated = new ArrayList<>();
        
        // First, add any system messages from the beginning
        for (ChatMessage message : messages) {
            if ("system".equals(message.getRole())) {
                truncated.add(message);
            } else {
                break; // Stop at first non-system message
            }
        }
        
        // Then add the most recent messages
        int remainingSlots = maxContextMessages - truncated.size();
        if (remainingSlots > 0) {
            int startIndex = Math.max(0, messages.size() - remainingSlots);
            for (int i = startIndex; i < messages.size(); i++) {
                ChatMessage message = messages.get(i);
                if (!"system".equals(message.getRole())) { // Avoid duplicating system messages
                    truncated.add(message);
                }
            }
        }
        
        return truncated;
    }
    
    private List<ChatMessage> truncateByTokenCount(List<ChatMessage> messages) {
        List<ChatMessage> truncated = new ArrayList<>();
        int currentTokenCount = 0;
        
        // Add messages from the end (most recent) until we hit the token limit
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            int messageTokens = geminiChatClient.estimateTokenCount(message.getContent());
            
            if (currentTokenCount + messageTokens <= maxContextTokens) {
                truncated.add(0, message); // Add to beginning to maintain order
                currentTokenCount += messageTokens;
            } else {
                // If this message would exceed the limit, try to truncate the message content
                String truncatedContent = truncateMessageContent(message.getContent(), 
                                                               maxContextTokens - currentTokenCount);
                if (!truncatedContent.isEmpty()) {
                    ChatMessage truncatedMessage = ChatMessage.builder()
                            .role(message.getRole())
                            .content(truncatedContent + "... [truncated]")
                            .timestamp(message.getTimestamp())
                            .functionsUsed(message.getFunctionsUsed())
                            .build();
                    truncated.add(0, truncatedMessage);
                }
                break; // Stop adding more messages
            }
        }
        
        return truncated;
    }
    
    private String truncateMessageContent(String content, int maxTokens) {
        if (maxTokens <= 0) {
            return "";
        }
        
        // Rough estimation: 4 characters per token
        int maxChars = maxTokens * 4;
        
        if (content.length() <= maxChars) {
            return content;
        }
        
        // Try to truncate at word boundaries
        String truncated = content.substring(0, Math.min(maxChars, content.length()));
        int lastSpace = truncated.lastIndexOf(' ');
        
        if (lastSpace > maxChars / 2) { // Only use word boundary if it's not too short
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated;
    }
    
    public String summarizeContext(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        
        // Create a simple summary of the conversation
        StringBuilder summary = new StringBuilder();
        summary.append("Previous conversation summary: ");
        
        int userMessages = 0;
        int assistantMessages = 0;
        
        for (ChatMessage message : messages) {
            switch (message.getRole()) {
                case "user":
                    userMessages++;
                    break;
                case "assistant":
                    assistantMessages++;
                    break;
            }
        }
        
        summary.append(String.format("User asked %d questions, assistant provided %d responses. ", 
                                    userMessages, assistantMessages));
        
        // Add the last few key exchanges
        List<ChatMessage> recentMessages = messages.subList(
                Math.max(0, messages.size() - 4), messages.size());
        
        for (ChatMessage message : recentMessages) {
            if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
                String shortContent = message.getContent().length() > 100 
                        ? message.getContent().substring(0, 100) + "..."
                        : message.getContent();
                summary.append(String.format("%s: %s ", 
                              message.getRole().equals("user") ? "User" : "Assistant", 
                              shortContent));
            }
        }
        
        return summary.toString();
    }
    
    private int estimateTotalTokens(List<ChatMessage> messages) {
        return messages.stream()
                .mapToInt(msg -> geminiChatClient.estimateTokenCount(msg.getContent()))
                .sum();
    }
    
    public boolean needsTruncation(ConversationContext context) {
        return context.getMessages().size() > maxContextMessages || 
               context.getTotalTokenCount() > maxContextTokens;
    }
    
    public List<ChatMessage> truncateIfNecessary(List<ChatMessage> messages, String sessionId) {
        // Create a temporary context to use existing truncation logic
        ConversationContext tempContext = ConversationContext.builder()
                .sessionId(sessionId)
                .messages(new ArrayList<>(messages))
                .totalTokenCount(estimateTotalTokens(messages))
                .build();
        
        if (needsTruncation(tempContext)) {
            return truncateContext(tempContext);
        }
        
        return new ArrayList<>(messages);
    }
}