package com.personal.backend.service;

import com.personal.backend.dto.ChatRequest;
import com.personal.backend.dto.ChatResponse;
import com.personal.backend.model.ChatMessage;
import com.personal.backend.model.ConversationContext;
import com.personal.backend.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final GeminiChatClient geminiChatClient;
    private final ConversationContextManager contextManager;
    private final ContextTruncationService truncationService;
    private final ToolRegistry toolRegistry;
    private final AuthenticationService authenticationService;
    
    public ChatResponse processMessage(ChatRequest request) {
        // Track concurrent requests
        contextManager.incrementActiveRequests();
        
        try {
            log.info("Processing chat message for session: {}", request.getSessionId());
            
            // Determine user context from authentication
            UserContext userContext = determineUserContext(request);
            
            // Get or create conversation context with write lock for updates
            ConversationContext context = contextManager.getOrCreateContext(
                    request.getSessionId(), 
                    userContext.username(), 
                    userContext.role()
            );
            
            // Get available functions based on user role
            List<FunctionCallback> availableFunctions = toolRegistry.getFunctionsForRole(userContext.role());
            
            log.debug("User {} has access to {} functions", userContext.username(), availableFunctions.size());
            
            // Truncate context if necessary
            List<ChatMessage> truncatedHistory = truncationService.truncateIfNecessary(
                    context.getMessages(), 
                    request.getMessage()
            );
            
            // Send message to Gemini with function calling
            String aiResponse = geminiChatClient.sendMessage(
                    request.getMessage(), 
                    truncatedHistory, 
                    availableFunctions
            );
            
            // Add user message to context
            ChatMessage userMessage = ChatMessage.builder()
                    .role("user")
                    .content(request.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            context.addMessage(userMessage);
            
            // Add AI response to context
            ChatMessage assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content(aiResponse)
                    .timestamp(LocalDateTime.now())
                    .build();
            context.addMessage(assistantMessage);
            
            // Update context with thread safety
            contextManager.updateContext(context);
            
            // Build response
            ChatResponse response = ChatResponse.builder()
                    .message(aiResponse)
                    .sessionId(context.getSessionId())
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .availableFunctions(toolRegistry.getAvailableFunctionNames(userContext.role()))
                    .build();
            
            log.info("Successfully processed chat message for user: {}", userContext.username());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
            
            return ChatResponse.builder()
                    .message("I apologize, but I encountered an error while processing your message. Please try again.")
                    .sessionId(request.getSessionId())
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .error("Internal server error: " + e.getMessage())
                    .build();
        } finally {
            // Always decrement active request count
            contextManager.decrementActiveRequests();
        }
    }
    
    public ChatResponse getConversationHistory(String sessionId, String authToken) {
        try {
            UserContext userContext = determineUserContext(authToken);
            
            ConversationContext context = contextManager.getContext(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found or expired"));
            
            // Verify user owns this session
            if (!userContext.username().equals(context.getUsername())) {
                throw new RuntimeException("Access denied to this conversation");
            }
            
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .conversationHistory(context.getMessages())
                    .availableFunctions(toolRegistry.getAvailableFunctionNames(userContext.role()))
                    .build();
            
        } catch (Exception e) {
            log.error("Error retrieving conversation history: {}", e.getMessage(), e);
            
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .error("Failed to retrieve conversation history: " + e.getMessage())
                    .build();
        }
    }
    
    public void clearConversation(String sessionId, String authToken) {
        try {
            UserContext userContext = determineUserContext(authToken);
            
            ConversationContext context = contextManager.getContext(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            
            // Verify user owns this session
            if (!userContext.username().equals(context.getUsername())) {
                throw new RuntimeException("Access denied to this conversation");
            }
            
            contextManager.removeContext(sessionId);
            log.info("Cleared conversation for session: {} (user: {})", sessionId, userContext.username());
            
        } catch (Exception e) {
            log.error("Error clearing conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear conversation: " + e.getMessage());
        }
    }
    
    private UserContext determineUserContext(ChatRequest request) {
        if (request.getAuthToken() != null && !request.getAuthToken().trim().isEmpty()) {
            return determineUserContext(request.getAuthToken());
        } else {
            // Guest user
            return new UserContext("guest", Role.GUEST);
        }
    }
    
    private UserContext determineUserContext(String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                return new UserContext("guest", Role.GUEST);
            }
            
            // Extract username from JWT token
            String username = authenticationService.extractUsernameFromToken(authToken);
            
            if (username == null) {
                log.warn("Invalid auth token provided, treating as guest");
                return new UserContext("guest", Role.GUEST);
            }
            
            // Get user role from authentication service
            Role userRole = authenticationService.getUserRole(username);
            
            return new UserContext(username, userRole);
            
        } catch (Exception e) {
            log.warn("Error determining user context from token, treating as guest: {}", e.getMessage());
            return new UserContext("guest", Role.GUEST);
        }
    }
    
    public ChatResponse healthCheck() {
        try {
            boolean geminiHealthy = geminiChatClient.isHealthy();
            int activeContexts = contextManager.getActiveContextCount();
            int totalFunctions = toolRegistry.getTotalFunctionCount();
            
            String healthMessage = String.format(
                    "Chat service is %s. Active contexts: %d, Available functions: %d", 
                    geminiHealthy ? "healthy" : "degraded", 
                    activeContexts, 
                    totalFunctions
            );
            
            return ChatResponse.builder()
                    .message(healthMessage)
                    .timestamp(LocalDateTime.now())
                    .success(geminiHealthy)
                    .build();
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            
            return ChatResponse.builder()
                    .message("Chat service health check failed")
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    // Helper record for user context
    private record UserContext(String username, Role role) {}
}