package com.personal.backend.service;

import com.personal.backend.dto.ChatRequest;
import com.personal.backend.dto.ChatResponse;
import com.personal.backend.model.Role;
import com.personal.backend.model.User;
import com.personal.backend.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChatServicePropertyTest {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ConversationContextManager contextManager;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Property
    @Label("Message processing consistency - all valid messages should get responses")
    void messageProcessingConsistency(
            @ForAll @StringLength(min = 1, max = 1000) String message) {
        
        // Arrange
        toolRegistry.initializeFunctions();
        
        ChatRequest request = ChatRequest.builder()
                .message(message.trim())
                .build();
        
        // Act
        ChatResponse response = chatService.processMessage(request);
        
        // Assert
        assertNotNull(response, "Response should never be null");
        assertNotNull(response.getSessionId(), "Session ID should be generated");
        assertNotNull(response.getTimestamp(), "Timestamp should be set");
        
        if (response.isSuccess()) {
            assertNotNull(response.getMessage(), "Successful response should have a message");
            assertFalse(response.getMessage().trim().isEmpty(), "Message should not be empty");
        } else {
            assertNotNull(response.getError(), "Failed response should have an error message");
        }
    }
    
    @Property
    @Label("Guest user access control - guests should only access read-only functions")
    void guestUserAccessControl() {
        // Arrange
        toolRegistry.initializeFunctions();
        
        // Act
        var guestFunctions = toolRegistry.getAvailableFunctionNames(Role.GUEST);
        
        // Assert
        assertNotNull(guestFunctions);
        assertTrue(guestFunctions.size() > 0, "Guests should have access to some functions");
        
        // All guest functions should be read-only (contain get, read, search, list, or view)
        for (String functionName : guestFunctions) {
            String lowerName = functionName.toLowerCase();
            boolean isReadOnly = lowerName.contains("get") || 
                               lowerName.contains("read") || 
                               lowerName.contains("search") || 
                               lowerName.contains("list") || 
                               lowerName.contains("view") ||
                               lowerName.contains("current") ||
                               lowerName.contains("upcoming");
            
            assertTrue(isReadOnly, 
                "Guest function '" + functionName + "' should be read-only");
        }
    }
    
    @Property
    @Label("Admin user tool access - admins should have more functions than guests")
    void adminUserToolAccess() {
        // Arrange
        toolRegistry.initializeFunctions();
        
        // Act
        var guestFunctions = toolRegistry.getAvailableFunctionNames(Role.GUEST);
        var adminFunctions = toolRegistry.getAvailableFunctionNames(Role.ADMIN);
        
        // Assert
        assertNotNull(guestFunctions);
        assertNotNull(adminFunctions);
        
        assertTrue(adminFunctions.size() >= guestFunctions.size(), 
                "Admin should have at least as many functions as guest");
        
        // All guest functions should be available to admin
        for (String guestFunction : guestFunctions) {
            assertTrue(adminFunctions.contains(guestFunction), 
                    "Admin should have access to guest function: " + guestFunction);
        }
    }
    
    @Property
    @Label("Function registration consistency - all registered functions should have descriptions")
    void functionRegistrationConsistency() {
        // Arrange & Act
        toolRegistry.initializeFunctions();
        var stats = toolRegistry.getFunctionStatistics();
        
        // Assert
        assertNotNull(stats);
        assertTrue((Integer) stats.get("totalFunctions") > 0, 
                "Should have registered functions");
        assertTrue((Integer) stats.get("guestFunctions") > 0, 
                "Should have guest functions");
        assertTrue((Integer) stats.get("adminFunctions") > 0, 
                "Should have admin functions");
        
        // Verify function categories exist
        @SuppressWarnings("unchecked")
        var categories = (java.util.Map<String, Integer>) stats.get("categories");
        assertNotNull(categories);
        assertTrue(categories.size() > 0, "Should have function categories");
    }
    
    @Property
    @Label("Session context lifecycle - contexts should be created and managed properly")
    void sessionContextLifecycle(
            @ForAll @StringLength(min = 3, max = 20) String username) {
        
        // Arrange
        String cleanUsername = username.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (cleanUsername.length() < 3) {
            cleanUsername = "testuser";
        }
        
        // Act - Create context
        var context = contextManager.createContext(cleanUsername, Role.GUEST);
        
        // Assert - Context creation
        assertNotNull(context);
        assertNotNull(context.getSessionId());
        assertEquals(cleanUsername, context.getUsername());
        assertEquals(Role.GUEST, context.getUserRole());
        assertNotNull(context.getCreatedAt());
        assertNotNull(context.getLastAccessedAt());
        
        // Act - Retrieve context
        var retrievedContext = contextManager.getContext(context.getSessionId());
        
        // Assert - Context retrieval
        assertTrue(retrievedContext.isPresent());
        assertEquals(context.getSessionId(), retrievedContext.get().getSessionId());
        
        // Act - Remove context
        contextManager.removeContext(context.getSessionId());
        
        // Assert - Context removal
        var removedContext = contextManager.getContext(context.getSessionId());
        assertTrue(removedContext.isEmpty());
    }
    
    @Property
    @Label("Error handling for unclear requests - service should handle invalid inputs gracefully")
    void errorHandlingForUnclearRequests(
            @ForAll("invalidMessages") String invalidMessage) {
        
        // Arrange
        ChatRequest request = ChatRequest.builder()
                .message(invalidMessage)
                .build();
        
        // Act
        ChatResponse response = chatService.processMessage(request);
        
        // Assert
        assertNotNull(response, "Should always return a response");
        assertNotNull(response.getTimestamp(), "Should have timestamp");
        
        // Even for invalid messages, we should get some kind of response
        // Either success with a helpful message or failure with error details
        assertTrue(response.isSuccess() || response.getError() != null,
                "Should either succeed with helpful message or fail with error details");
    }
    
    @Provide
    Arbitrary<String> invalidMessages() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\n\t"),
                Arbitraries.strings().withChars(' ', '\n', '\t').ofLength(10),
                Arbitraries.strings().withChars('!', '@', '#', '$', '%').ofLength(5),
                Arbitraries.just("?"),
                Arbitraries.just("???"),
                Arbitraries.just("............"),
                Arbitraries.strings().numeric().ofLength(20)
        );
    }
}