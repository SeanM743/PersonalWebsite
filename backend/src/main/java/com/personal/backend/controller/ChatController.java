package com.personal.backend.controller;

import com.personal.backend.dto.ChatRequest;
import com.personal.backend.dto.ChatResponse;
import com.personal.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            log.info("Received chat request for session: {}", request.getSessionId());
            
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                ChatResponse errorResponse = ChatResponse.builder()
                        .message("Message cannot be empty")
                        .sessionId(request.getSessionId())
                        .success(false)
                        .error("Invalid request: empty message")
                        .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ChatResponse response = chatService.processMessage(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error in chat endpoint: {}", e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.builder()
                    .message("An unexpected error occurred. Please try again.")
                    .sessionId(request.getSessionId())
                    .success(false)
                    .error("Internal server error")
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ChatResponse> getConversationHistory(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            log.info("Retrieving conversation history for session: {}", sessionId);
            
            ChatResponse response = chatService.getConversationHistory(sessionId, authToken);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving conversation history: {}", e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.builder()
                    .sessionId(sessionId)
                    .success(false)
                    .error("Failed to retrieve conversation history")
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Void> clearConversation(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            log.info("Clearing conversation for session: {}", sessionId);
            
            chatService.clearConversation(sessionId, authToken);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error clearing conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ChatResponse> healthCheck() {
        try {
            ChatResponse response = chatService.healthCheck();
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.builder()
                    .message("Health check failed")
                    .success(false)
                    .error("Service unavailable")
                    .build();
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
    
    @GetMapping("/functions")
    public ResponseEntity<?> getAvailableFunctions(
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            // This endpoint could be expanded to show available functions based on user role
            // For now, return a simple message
            return ResponseEntity.ok().body(
                "Available functions depend on your authentication level. " +
                "Guest users have access to read-only functions, while authenticated users have additional capabilities."
            );
            
        } catch (Exception e) {
            log.error("Error retrieving available functions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve available functions");
        }
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ChatResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid request: {}", e.getMessage());
        
        ChatResponse errorResponse = ChatResponse.builder()
                .message("Invalid request: " + e.getMessage())
                .success(false)
                .error("Bad request")
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ChatResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error in chat controller: {}", e.getMessage(), e);
        
        ChatResponse errorResponse = ChatResponse.builder()
                .message("An error occurred while processing your request")
                .success(false)
                .error("Internal server error")
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}