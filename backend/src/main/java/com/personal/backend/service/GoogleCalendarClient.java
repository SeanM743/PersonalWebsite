package com.personal.backend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.personal.backend.config.GoogleCalendarConfiguration;
import com.personal.backend.util.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarClient {
    
    private final GoogleCalendarAuthManager authManager;
    private final GoogleCalendarConfiguration.GoogleCalendarProperties properties;
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final AtomicReference<Calendar> cachedCalendarService = new AtomicReference<>();
    private final AtomicReference<NetHttpTransport> cachedHttpTransport = new AtomicReference<>();
    
    /**
     * Get authenticated Google Calendar service with automatic retry and refresh
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Calendar getCalendarService() throws IOException, GeneralSecurityException {
        Calendar service = cachedCalendarService.get();
        
        // Check if we need to recreate the service
        if (service == null || needsServiceRefresh()) {
            synchronized (this) {
                service = cachedCalendarService.get();
                if (service == null || needsServiceRefresh()) {
                    service = createCalendarService();
                    cachedCalendarService.set(service);
                }
            }
        }
        
        return service;
    }
    
    /**
     * Check if Google Calendar service is available
     */
    public boolean isServiceAvailable() {
        try {
            Calendar service = getCalendarService();
            return service != null && authManager.areCredentialsAvailable();
        } catch (Exception e) {
            log.debug("Google Calendar service not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform a health check on the Google Calendar service
     */
    public HealthCheckResult performHealthCheck() {
        try {
            // Check credentials first
            if (!authManager.areCredentialsAvailable()) {
                return HealthCheckResult.builder()
                        .healthy(false)
                        .message("Google Calendar credentials not available")
                        .details("Please check credential configuration and file path")
                        .build();
            }
            
            // Validate credentials
            ValidationResult credentialValidation = authManager.validateCredentials();
            if (!credentialValidation.isValid()) {
                return HealthCheckResult.builder()
                        .healthy(false)
                        .message("Google Calendar credential validation failed")
                        .details(String.join(", ", credentialValidation.getErrors()))
                        .build();
            }
            
            // Try to create service
            Calendar service = getCalendarService();
            if (service == null) {
                return HealthCheckResult.builder()
                        .healthy(false)
                        .message("Failed to create Google Calendar service")
                        .details("Service creation returned null")
                        .build();
            }
            
            // Try a simple API call (list calendars)
            try {
                service.calendarList().list().setMaxResults(1).execute();
                
                return HealthCheckResult.builder()
                        .healthy(true)
                        .message("Google Calendar service is healthy")
                        .details("Successfully connected to Google Calendar API")
                        .build();
                        
            } catch (Exception apiException) {
                return HealthCheckResult.builder()
                        .healthy(false)
                        .message("Google Calendar API call failed")
                        .details("API Error: " + apiException.getMessage())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return HealthCheckResult.builder()
                    .healthy(false)
                    .message("Health check failed with exception")
                    .details(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Get service information for monitoring
     */
    public ServiceInfo getServiceInfo() {
        GoogleCalendarAuthManager.CredentialInfo credInfo = authManager.getCredentialInfo();
        
        return ServiceInfo.builder()
                .serviceAvailable(isServiceAvailable())
                .credentialsAvailable(credInfo.isAvailable())
                .serviceAccountEmail(credInfo.getServiceAccountEmail())
                .projectId(credInfo.getProjectId())
                .applicationName(properties.getApplicationName())
                .defaultCalendarId(properties.getDefaultCalendarId())
                .apiTimeout(properties.getApiTimeout())
                .lastCredentialLoad(credInfo.getLastLoaded())
                .build();
    }
    
    /**
     * Force refresh of the calendar service
     */
    public void refreshService() throws IOException, GeneralSecurityException {
        synchronized (this) {
            log.info("Forcing refresh of Google Calendar service");
            cachedCalendarService.set(null);
            cachedHttpTransport.set(null);
            authManager.reloadCredentials();
            
            // Recreate service
            createCalendarService();
        }
    }
    
    /**
     * Clear cached service (useful for testing)
     */
    public void clearService() {
        synchronized (this) {
            cachedCalendarService.set(null);
            cachedHttpTransport.set(null);
            authManager.clearCredentials();
            log.info("Cleared cached Google Calendar service");
        }
    }
    
    private Calendar createCalendarService() throws IOException, GeneralSecurityException {
        log.debug("Creating new Google Calendar service");
        
        // Get authenticated credentials
        GoogleCredentials credentials = authManager.getCredentials();
        if (credentials == null) {
            throw new IOException("No valid credentials available for Google Calendar service");
        }
        
        // Get or create HTTP transport
        NetHttpTransport httpTransport = getHttpTransport();
        
        // Create Calendar service
        Calendar service = new Calendar.Builder(
                httpTransport, 
                JSON_FACTORY, 
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(properties.getApplicationName())
                .build();
        
        log.info("Successfully created Google Calendar service for application: {}", 
                properties.getApplicationName());
        
        return service;
    }
    
    private NetHttpTransport getHttpTransport() throws GeneralSecurityException, IOException {
        NetHttpTransport transport = cachedHttpTransport.get();
        
        if (transport == null) {
            synchronized (this) {
                transport = cachedHttpTransport.get();
                if (transport == null) {
                    log.debug("Creating new HTTP transport");
                    transport = GoogleNetHttpTransport.newTrustedTransport();
                    cachedHttpTransport.set(transport);
                }
            }
        }
        
        return transport;
    }
    
    private boolean needsServiceRefresh() {
        // Check if credentials need refresh
        return !authManager.areCredentialsAvailable();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class HealthCheckResult {
        private boolean healthy;
        private String message;
        private String details;
        private java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ServiceInfo {
        private boolean serviceAvailable;
        private boolean credentialsAvailable;
        private String serviceAccountEmail;
        private String projectId;
        private String applicationName;
        private String defaultCalendarId;
        private int apiTimeout;
        private java.time.LocalDateTime lastCredentialLoad;
        private java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
    }
}