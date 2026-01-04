package com.personal.backend.service;

import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.personal.backend.util.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarAuthManager {
    
    private final ResourceLoader resourceLoader;
    
    @Value("${google.calendar.credentials.file.path}")
    private String credentialsFilePath;
    
    private final AtomicReference<GoogleCredentials> cachedCredentials = new AtomicReference<>();
    private volatile LocalDateTime lastCredentialLoad;
    private volatile boolean credentialsAvailable = false;
    
    // Cache credentials for 50 minutes (Google tokens expire after 1 hour)
    private static final long CREDENTIAL_CACHE_MINUTES = 50;
    
    /**
     * Get authenticated Google credentials with automatic refresh
     */
    public GoogleCredentials getCredentials() throws IOException {
        GoogleCredentials credentials = cachedCredentials.get();
        
        // Check if we need to refresh credentials
        if (credentials == null || needsRefresh()) {
            synchronized (this) {
                // Double-check pattern
                credentials = cachedCredentials.get();
                if (credentials == null || needsRefresh()) {
                    credentials = loadAndCacheCredentials();
                }
            }
        }
        
        // Refresh token if needed
        if (credentials != null && needsTokenRefresh(credentials)) {
            try {
                credentials.refresh();
                log.debug("Refreshed Google Calendar credentials token");
            } catch (IOException e) {
                log.warn("Failed to refresh credentials token, will reload: {}", e.getMessage());
                credentials = loadAndCacheCredentials();
            }
        }
        
        return credentials;
    }
    
    /**
     * Check if credentials are available and valid
     */
    public boolean areCredentialsAvailable() {
        try {
            GoogleCredentials credentials = getCredentials();
            return credentials != null && credentialsAvailable;
        } catch (Exception e) {
            log.debug("Credentials not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate credentials by attempting to use them
     */
    public ValidationResult validateCredentials() {
        try {
            GoogleCredentials credentials = getCredentials();
            
            if (credentials == null) {
                return ValidationResult.builder()
                        .valid(false)
                        .errors(Collections.singletonList("No credentials loaded"))
                        .build();
            }
            
            // Check if credentials are service account credentials
            if (!(credentials instanceof ServiceAccountCredentials)) {
                return ValidationResult.builder()
                        .valid(false)
                        .errors(Collections.singletonList("Credentials must be service account credentials"))
                        .build();
            }
            
            ServiceAccountCredentials serviceCredentials = (ServiceAccountCredentials) credentials;
            
            // Validate required scopes
            if (!credentials.createScoped(Collections.singletonList(CalendarScopes.CALENDAR)).equals(credentials)) {
                log.debug("Credentials will be scoped for Calendar API");
            }
            
            // Try to refresh to validate
            credentials.refresh();
            
            log.info("Google Calendar credentials validated successfully for service account: {}", 
                    serviceCredentials.getClientEmail());
            
            return ValidationResult.builder()
                    .valid(true)
                    .build();
            
        } catch (IOException e) {
            log.error("Credential validation failed: {}", e.getMessage());
            return ValidationResult.builder()
                    .valid(false)
                    .errors(Collections.singletonList("Credential validation failed: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during credential validation: {}", e.getMessage(), e);
            return ValidationResult.builder()
                    .valid(false)
                    .errors(Collections.singletonList("Unexpected validation error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get credential information for debugging/monitoring
     */
    public CredentialInfo getCredentialInfo() {
        try {
            GoogleCredentials credentials = cachedCredentials.get();
            if (credentials instanceof ServiceAccountCredentials) {
                ServiceAccountCredentials serviceCredentials = (ServiceAccountCredentials) credentials;
                
                return CredentialInfo.builder()
                        .available(true)
                        .serviceAccountEmail(serviceCredentials.getClientEmail())
                        .projectId(serviceCredentials.getProjectId())
                        .lastLoaded(lastCredentialLoad)
                        .tokenExpired(needsTokenRefresh(credentials))
                        .credentialsPath(credentialsFilePath)
                        .build();
            }
        } catch (Exception e) {
            log.debug("Error getting credential info: {}", e.getMessage());
        }
        
        return CredentialInfo.builder()
                .available(false)
                .credentialsPath(credentialsFilePath)
                .build();
    }
    
    /**
     * Force reload of credentials (useful for testing or credential updates)
     */
    public void reloadCredentials() throws IOException {
        synchronized (this) {
            cachedCredentials.set(null);
            lastCredentialLoad = null;
            credentialsAvailable = false;
            
            log.info("Forcing reload of Google Calendar credentials");
            loadAndCacheCredentials();
        }
    }
    
    /**
     * Clear cached credentials
     */
    public void clearCredentials() {
        synchronized (this) {
            cachedCredentials.set(null);
            lastCredentialLoad = null;
            credentialsAvailable = false;
            log.info("Cleared cached Google Calendar credentials");
        }
    }
    
    private GoogleCredentials loadAndCacheCredentials() throws IOException {
        log.info("Loading Google Calendar credentials from: {}", credentialsFilePath);
        
        try {
            InputStream credentialsStream = getCredentialsStream();
            
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));
            
            // Cache the credentials
            cachedCredentials.set(credentials);
            lastCredentialLoad = LocalDateTime.now();
            credentialsAvailable = true;
            
            if (credentials instanceof ServiceAccountCredentials) {
                ServiceAccountCredentials serviceCredentials = (ServiceAccountCredentials) credentials;
                log.info("Successfully loaded service account credentials for: {}", 
                        serviceCredentials.getClientEmail());
            } else {
                log.info("Successfully loaded Google Calendar credentials");
            }
            
            return credentials;
            
        } catch (IOException e) {
            credentialsAvailable = false;
            log.error("Failed to load Google Calendar credentials from: {}", credentialsFilePath, e);
            
            // Provide helpful error messages
            if (e.getMessage().contains("FileNotFoundException") || e.getMessage().contains("No such file")) {
                throw new IOException("Credentials file not found at: " + credentialsFilePath + 
                        ". Please ensure the file exists and the path is correct.", e);
            } else if (e.getMessage().contains("JSON")) {
                throw new IOException("Invalid credentials file format. Please ensure it's a valid JSON service account key.", e);
            } else {
                throw new IOException("Failed to load credentials: " + e.getMessage(), e);
            }
        }
    }
    
    private InputStream getCredentialsStream() throws IOException {
        // Try to load as a resource first (for classpath resources)
        try {
            Resource resource = resourceLoader.getResource("classpath:" + credentialsFilePath);
            if (resource.exists()) {
                log.debug("Loading credentials from classpath resource");
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.debug("Could not load from classpath, trying file path: {}", e.getMessage());
        }
        
        // Try as absolute file path
        try {
            log.debug("Loading credentials from file path");
            return new FileInputStream(credentialsFilePath);
        } catch (Exception e) {
            // Try relative to working directory
            String workingDir = System.getProperty("user.dir");
            String relativePath = workingDir + "/" + credentialsFilePath;
            log.debug("Trying relative path: {}", relativePath);
            return new FileInputStream(relativePath);
        }
    }
    
    private boolean needsRefresh() {
        return lastCredentialLoad == null || 
               lastCredentialLoad.isBefore(LocalDateTime.now().minusMinutes(CREDENTIAL_CACHE_MINUTES));
    }
    
    private boolean needsTokenRefresh(GoogleCredentials credentials) {
        try {
            // Check if token is expired or will expire soon (within 5 minutes)
            return credentials.getAccessToken() == null || 
                   credentials.getAccessToken().getExpirationTime() == null ||
                   credentials.getAccessToken().getExpirationTime().before(
                           new java.util.Date(System.currentTimeMillis() + 5 * 60 * 1000));
        } catch (Exception e) {
            log.debug("Error checking token expiration: {}", e.getMessage());
            return true; // Assume refresh needed if we can't check
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CredentialInfo {
        private boolean available;
        private String serviceAccountEmail;
        private String projectId;
        private LocalDateTime lastLoaded;
        private boolean tokenExpired;
        private String credentialsPath;
    }
}