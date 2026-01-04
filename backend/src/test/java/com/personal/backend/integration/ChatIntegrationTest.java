package com.personal.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.backend.dto.ChatRequest;
import com.personal.backend.dto.ChatResponse;
import com.personal.backend.model.Role;
import com.personal.backend.model.User;
import com.personal.backend.repository.UserRepository;
import com.personal.backend.service.AuthenticationService;
import com.personal.backend.service.ConversationContextManager;
import com.personal.backend.service.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class ChatIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ConversationContextManager contextManager;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    private String adminToken;
    private String guestToken;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test users
        userRepository.deleteAll();
        
        // Create test admin user
        User adminUser = authenticationService.createUser("testadmin", "password123", Role.ADMIN);
        adminToken = authenticationService.authenticateUser("testadmin", "password123");
        
        // Initialize tool registry
        toolRegistry.initializeFunctions();
    }
    
    @Test
    void testGuestChatBasicFunctionality() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Hello, can you help me?")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ChatResponse.class
        );
        
        assertTrue(response.isSuccess());
        assertNotNull(response.getMessage());
        assertNotNull(response.getSessionId());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getAvailableFunctions());
        
        // Guest should have access to read-only functions
        assertTrue(response.getAvailableFunctions().size() > 0);
    }
    
    @Test
    void testAuthenticatedChatWithMoreFunctions() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("What functions do I have access to?")
                .authToken(adminToken)
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ChatResponse.class
        );
        
        assertTrue(response.isSuccess());
        assertNotNull(response.getMessage());
        assertNotNull(response.getAvailableFunctions());
        
        // Admin should have access to more functions than guest
        int adminFunctionCount = toolRegistry.getFunctionCountForRole(Role.ADMIN);
        int guestFunctionCount = toolRegistry.getFunctionCountForRole(Role.GUEST);
        
        assertTrue(adminFunctionCount >= guestFunctionCount);
    }
    
    @Test
    void testConversationHistory() throws Exception {
        // Send first message
        ChatRequest request1 = ChatRequest.builder()
                .message("Hello, my name is John")
                .build();
        
        MvcResult result1 = mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChatResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), 
                ChatResponse.class
        );
        
        String sessionId = response1.getSessionId();
        
        // Send second message with same session
        ChatRequest request2 = ChatRequest.builder()
                .message("What's my name?")
                .sessionId(sessionId)
                .build();
        
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
        
        // Get conversation history
        mockMvc.perform(get("/api/chat/history/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.conversationHistory").isArray());
    }
    
    @Test
    void testClearConversation() throws Exception {
        // Create a conversation
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ChatResponse.class
        );
        
        String sessionId = response.getSessionId();
        
        // Clear the conversation
        mockMvc.perform(delete("/api/chat/history/" + sessionId))
                .andExpect(status().isNoContent());
        
        // Try to get history - should fail
        mockMvc.perform(get("/api/chat/history/" + sessionId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testInvalidRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("") // Empty message
                .build();
        
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testFunctionRegistryStatistics() {
        // Test that functions are properly registered
        int totalFunctions = toolRegistry.getTotalFunctionCount();
        assertTrue(totalFunctions > 0);
        
        int guestFunctions = toolRegistry.getFunctionCountForRole(Role.GUEST);
        int adminFunctions = toolRegistry.getFunctionCountForRole(Role.ADMIN);
        
        assertTrue(guestFunctions > 0);
        assertTrue(adminFunctions >= guestFunctions);
        
        // Test function statistics
        var stats = toolRegistry.getFunctionStatistics();
        assertNotNull(stats);
        assertTrue((Integer) stats.get("totalFunctions") > 0);
    }
    
    @Test
    void testContextManagerCleanup() {
        // Test context creation and cleanup
        int initialContexts = contextManager.getActiveContextCount();
        
        var context = contextManager.createContext("testuser", Role.GUEST);
        assertEquals(initialContexts + 1, contextManager.getActiveContextCount());
        
        contextManager.removeContext(context.getSessionId());
        assertEquals(initialContexts, contextManager.getActiveContextCount());
    }
}