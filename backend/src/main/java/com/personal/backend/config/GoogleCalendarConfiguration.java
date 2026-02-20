package com.personal.backend.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class GoogleCalendarConfiguration {
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    @Value("${google.calendar.credentials.file.path}")
    private String credentialsFilePath;
    
    @Value("${google.calendar.application.name}")
    private String applicationName;
    
    private final ResourceLoader resourceLoader;
    
    public GoogleCalendarConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @Bean
    public NetHttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }
    
    @Bean
    public JsonFactory jsonFactory() {
        return JSON_FACTORY;
    }
    
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try {
            log.info("Loading Google Calendar credentials from: {}", credentialsFilePath);
            
            InputStream credentialsStream;
            
            // Try to load as a resource first (for classpath resources)
            try {
                Resource resource = resourceLoader.getResource("classpath:" + credentialsFilePath);
                if (resource.exists()) {
                    credentialsStream = resource.getInputStream();
                    log.debug("Loaded credentials from classpath resource");
                } else {
                    // Try as file path
                    credentialsStream = new FileInputStream(credentialsFilePath);
                    log.debug("Loaded credentials from file path");
                }
            } catch (Exception e) {
                // Fallback to file path
                credentialsStream = new FileInputStream(credentialsFilePath);
                log.debug("Loaded credentials from file path (fallback)");
            }
            
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));
            
            log.info("Successfully loaded Google Calendar credentials");
            return credentials;
            
        } catch (IOException e) {
            log.error("Failed to load Google Calendar credentials from: {}", credentialsFilePath, e);
            log.warn("Google Calendar integration will be disabled. To enable:");
            log.warn("1. Create a service account in Google Cloud Console");
            log.warn("2. Download the JSON credentials file");
            log.warn("3. Set GOOGLE_CALENDAR_CREDENTIALS_PATH environment variable");
            log.warn("4. Or place the file at: {}", credentialsFilePath);
            
            // Return null to indicate credentials are not available
            // Services should handle this gracefully
            return null;
        }
    }
    
    @Bean
    public Calendar googleCalendarService(
            NetHttpTransport httpTransport,
            JsonFactory jsonFactory,
            GoogleCredentials googleCredentials) throws IOException {
        
        if (googleCredentials == null) {
            log.warn("Google Calendar service not available - credentials not loaded");
            return null;
        }
        
        try {
            return new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(googleCredentials))
                    .setApplicationName(applicationName)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to create Google Calendar service", e);
            return null;
        }
    }
    
    @Bean
    public GoogleCalendarProperties googleCalendarProperties(
            @Value("${google.calendar.default.calendar.id}") String defaultCalendarId,
            @Value("${google.calendar.additional.calendar.ids:}") String additionalCalendarIds,
            @Value("${google.calendar.api.timeout}") int apiTimeout,
            @Value("${google.calendar.cache.ttl.minutes}") int cacheTtlMinutes,
            @Value("${google.calendar.max.events.per.request}") int maxEventsPerRequest) {
        
        List<String> additionalIds = additionalCalendarIds.isEmpty()
                ? Collections.emptyList()
                : Arrays.stream(additionalCalendarIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
        
        return GoogleCalendarProperties.builder()
                .defaultCalendarId(defaultCalendarId)
                .additionalCalendarIds(additionalIds)
                .apiTimeout(apiTimeout)
                .cacheTtlMinutes(cacheTtlMinutes)
                .maxEventsPerRequest(maxEventsPerRequest)
                .credentialsFilePath(credentialsFilePath)
                .applicationName(applicationName)
                .build();
    }
    
    // Configuration properties holder
    @lombok.Data
    @lombok.Builder
    public static class GoogleCalendarProperties {
        private String defaultCalendarId;
        private List<String> additionalCalendarIds;
        private int apiTimeout;
        private int cacheTtlMinutes;
        private int maxEventsPerRequest;
        private String credentialsFilePath;
        private String applicationName;
        
        /** Returns all calendar IDs to query (default + additional) */
        public List<String> getAllCalendarIds() {
            List<String> all = new java.util.ArrayList<>();
            all.add(defaultCalendarId);
            if (additionalCalendarIds != null) {
                all.addAll(additionalCalendarIds);
            }
            return all;
        }
    }
}