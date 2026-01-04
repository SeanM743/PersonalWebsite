package com.personal.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.backend.dto.AuthResponse;
import com.personal.backend.dto.LoginRequest;
import com.personal.backend.model.Role;
import com.personal.backend.model.User;
import com.personal.backend.repository.UserRepository;
import com.personal.backend.service.AuthenticationService;
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
public class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    public void setUp() {
        // Clean up any existing test users
        userRepository.deleteAll();
        
        // Create test user
        testUser = authenticationService.createUser("testuser", "password123", Role.GUEST);
    }
    
    @Test
    public void testSuccessfulLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);
        
        assertNotNull(authResponse.getToken());
        assertEquals("testuser", authResponse.getUsername());
        assertEquals(Role.GUEST, authResponse.getRole());
    }
    
    @Test
    public void testInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }
    
    @Test
    public void testNonExistentUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }
    
    @Test
    public void testValidationErrors() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("") // Invalid: empty username
                .password("123") // Invalid: too short password
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
    
    @Test
    public void testMalformedRequest() throws Exception {
        String malformedJson = "{\"username\":\"test\",\"password\":}"; // Invalid JSON
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    public void testAdminUserLogin() throws Exception {
        // Create admin user
        User adminUser = authenticationService.createUser("admin", "admin123", Role.ADMIN);
        
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);
        
        assertEquals(Role.ADMIN, authResponse.getRole());
    }
    
    @Test
    public void testTokenValidationEndpoint() throws Exception {
        // First, login to get a token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String loginResponseContent = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(loginResponseContent, AuthResponse.class);
        String token = authResponse.getToken();
        
        // Test token validation
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }
    
    @Test
    public void testTokenValidationWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }
    
    @Test
    public void testTokenValidationWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid authorization header"));
    }
    
    @Test
    public void testDatabasePersistence() {
        // Verify user was persisted to database
        assertTrue(userRepository.existsByUsername("testuser"));
        
        User savedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals(Role.GUEST, savedUser.getRole());
        assertNotNull(savedUser.getPasswordHash());
        assertNotEquals("password123", savedUser.getPasswordHash()); // Should be hashed
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }
}