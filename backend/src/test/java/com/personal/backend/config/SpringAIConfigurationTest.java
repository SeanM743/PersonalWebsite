package com.personal.backend.config;

import com.personal.backend.service.GeminiChatClient;
import com.personal.backend.service.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SpringAIConfigurationTest {
    
    @Autowired(required = false)
    private ChatModel chatModel;
    
    @Autowired
    private GeminiChatClient geminiChatClient;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private GuestFunctionConfiguration guestFunctionConfiguration;
    
    @Autowired
    private AdminFunctionConfiguration adminFunctionConfiguration;
    
    @Test
    void testChatModelConfiguration() {
        // In test profile, ChatModel might not be available due to missing credentials
        // This test validates that the configuration doesn't break the application startup
        assertNotNull(geminiChatClient, "GeminiChatClient should be configured");
    }
    
    @Test
    void testFunctionRegistration() {
        // Initialize the tool registry
        toolRegistry.initializeFunctions();
        
        // Verify functions are registered
        int totalFunctions = toolRegistry.getTotalFunctionCount();
        assertTrue(totalFunctions > 0, "Should have registered functions");
        
        // Verify function statistics
        var stats = toolRegistry.getFunctionStatistics();
        assertNotNull(stats);
        assertTrue((Integer) stats.get("totalFunctions") > 0);
        assertTrue((Integer) stats.get("guestFunctions") > 0);
        assertTrue((Integer) stats.get("adminFunctions") > 0);
        
        // Verify categories exist
        @SuppressWarnings("unchecked")
        var categories = (java.util.Map<String, Integer>) stats.get("categories");
        assertNotNull(categories);
        assertTrue(categories.size() > 0);
    }
    
    @Test
    void testGuestFunctionConfiguration() {
        assertNotNull(guestFunctionConfiguration, "GuestFunctionConfiguration should be configured");
        
        // Test that guest functions are properly configured
        var getRecentPosts = guestFunctionConfiguration.getRecentPosts();
        assertNotNull(getRecentPosts, "getRecentPosts function should be configured");
        
        var getQuickFacts = guestFunctionConfiguration.getQuickFactsGuest();
        assertNotNull(getQuickFacts, "getQuickFactsGuest function should be configured");
        
        var getCurrentActivities = guestFunctionConfiguration.getCurrentActivities();
        assertNotNull(getCurrentActivities, "getCurrentActivities function should be configured");
        
        var getUpcomingTrips = guestFunctionConfiguration.getUpcomingTrips();
        assertNotNull(getUpcomingTrips, "getUpcomingTrips function should be configured");
        
        var searchActivities = guestFunctionConfiguration.searchActivities();
        assertNotNull(searchActivities, "searchActivities function should be configured");
    }
    
    @Test
    void testAdminFunctionConfiguration() {
        assertNotNull(adminFunctionConfiguration, "AdminFunctionConfiguration should be configured");
        
        // Test that admin functions are properly configured
        var createPost = adminFunctionConfiguration.createPost();
        assertNotNull(createPost, "createPost function should be configured");
        
        var addQuickFact = adminFunctionConfiguration.addQuickFact();
        assertNotNull(addQuickFact, "addQuickFact function should be configured");
        
        var deletePost = adminFunctionConfiguration.deletePost();
        assertNotNull(deletePost, "deletePost function should be configured");
        
        var updateQuickFact = adminFunctionConfiguration.updateQuickFactAdmin();
        assertNotNull(updateQuickFact, "updateQuickFactAdmin function should be configured");
        
        var deleteQuickFact = adminFunctionConfiguration.deleteQuickFact();
        assertNotNull(deleteQuickFact, "deleteQuickFact function should be configured");
    }
    
    @Test
    void testFunctionCallbackCreation() {
        // Initialize functions
        toolRegistry.initializeFunctions();
        
        // Test that function callbacks can be created
        var guestFunctions = toolRegistry.getFunctionsForRole(com.personal.backend.model.Role.GUEST);
        assertNotNull(guestFunctions);
        assertTrue(guestFunctions.size() > 0, "Should have guest functions");
        
        var adminFunctions = toolRegistry.getFunctionsForRole(com.personal.backend.model.Role.ADMIN);
        assertNotNull(adminFunctions);
        assertTrue(adminFunctions.size() > 0, "Should have admin functions");
        
        // Admin should have at least as many functions as guest
        assertTrue(adminFunctions.size() >= guestFunctions.size(), 
                "Admin should have at least as many functions as guest");
    }
    
    @Test
    void testFunctionPermissions() {
        toolRegistry.initializeFunctions();
        
        // Test guest permissions
        assertTrue(toolRegistry.isFunctionAvailable("getRecentPosts", com.personal.backend.model.Role.GUEST));
        assertTrue(toolRegistry.isFunctionAvailable("getQuickFacts", com.personal.backend.model.Role.GUEST));
        assertTrue(toolRegistry.isFunctionAvailable("getCurrentActivities", com.personal.backend.model.Role.GUEST));
        
        // Test admin permissions (should have all guest functions plus more)
        assertTrue(toolRegistry.isFunctionAvailable("getRecentPosts", com.personal.backend.model.Role.ADMIN));
        assertTrue(toolRegistry.isFunctionAvailable("createPost", com.personal.backend.model.Role.ADMIN));
        assertTrue(toolRegistry.isFunctionAvailable("addQuickFact", com.personal.backend.model.Role.ADMIN));
        
        // Test that write functions are admin-only
        assertFalse(toolRegistry.isFunctionAvailable("createPost", com.personal.backend.model.Role.GUEST));
        assertFalse(toolRegistry.isFunctionAvailable("addQuickFact", com.personal.backend.model.Role.GUEST));
        assertFalse(toolRegistry.isFunctionAvailable("deletePost", com.personal.backend.model.Role.GUEST));
    }
    
    @Test
    void testGeminiClientHealthCheck() {
        // Test that health check doesn't throw exceptions
        // Note: This may fail in test environment without proper Gemini credentials
        // but should not break the application
        assertDoesNotThrow(() -> {
            boolean isHealthy = geminiChatClient.isHealthy();
            // In test environment, this might be false due to missing credentials
            // The important thing is that it doesn't throw an exception
        });
    }
    
    @Test
    void testTokenEstimation() {
        // Test token estimation functionality
        int tokenCount = geminiChatClient.estimateTokenCount("Hello world");
        assertTrue(tokenCount > 0, "Should estimate positive token count");
        
        int longerTokenCount = geminiChatClient.estimateTokenCount("This is a much longer message that should have more tokens");
        assertTrue(longerTokenCount > tokenCount, "Longer message should have more tokens");
    }
}