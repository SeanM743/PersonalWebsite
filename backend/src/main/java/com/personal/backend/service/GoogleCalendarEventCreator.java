package com.personal.backend.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.personal.backend.config.GoogleCalendarConfiguration;
import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.util.ValidationResult;
import com.personal.backend.model.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarEventCreator {
    
    private final GoogleCalendarClient calendarClient;
    private final GoogleCalendarEventManager eventManager;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;
    private final GoogleCalendarConfiguration.GoogleCalendarProperties properties;
    
    /**
     * Create a new calendar event with comprehensive validation
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> createEvent(EventRequest eventRequest) {
        try {
            log.info("Creating calendar event: {}", eventRequest.getTitle());
            
            // Validate service availability
            if (!calendarClient.isServiceAvailable()) {
                return CalendarResponse.error("Google Calendar service is not available. Please check your credentials.");
            }
            
            // Validate event request
            ValidationResult validation = eventValidator.validateEventRequest(eventRequest);
            if (!validation.isValid()) {
                String errorMessage = "Event validation failed: " + String.join(", ", validation.getErrors());
                log.warn("Event creation validation failed: {}", errorMessage);
                return CalendarResponse.validationError(errorMessage);
            }
            
            // Log warnings if any
            if (validation.getWarnings() != null && !validation.getWarnings().isEmpty()) {
                log.info("Event creation warnings: {}", String.join(", ", validation.getWarnings()));
            }
            
            // Check for scheduling conflicts if requested
            CalendarResponse<List<CalendarEvent>> conflictCheck = eventManager.checkForConflicts(eventRequest);
            if (conflictCheck.isSuccess() && !conflictCheck.getData().isEmpty()) {
                log.info("Found {} potential scheduling conflicts for new event", conflictCheck.getData().size());
                // Continue with creation but include conflict info in metadata
            }
            
            // Convert to Google Calendar event
            Event googleEvent = eventMapper.fromEventRequestToGoogle(eventRequest);
            if (googleEvent == null) {
                return CalendarResponse.error("Failed to convert event request to Google Calendar format");
            }
            
            // Get calendar service and create event
            Calendar service = calendarClient.getCalendarService();
            String calendarId = eventRequest.getCalendarId() != null ? 
                    eventRequest.getCalendarId() : properties.getDefaultCalendarId();
            
            // Execute creation
            Event createdEvent = service.events().insert(calendarId, googleEvent).execute();
            
            if (createdEvent == null || createdEvent.getId() == null) {
                return CalendarResponse.error("Event creation failed - no event ID returned");
            }
            
            // Convert back to internal model
            CalendarEvent calendarEvent = eventMapper.fromGoogleEvent(createdEvent);
            if (calendarEvent == null) {
                return CalendarResponse.error("Failed to convert created event data");
            }
            
            calendarEvent.setCalendarId(calendarId);
            
            log.info("Successfully created calendar event: {} (ID: {})", 
                    calendarEvent.getTitle(), calendarEvent.getId());
            
            // Build response with metadata
            Map<String, Object> metadata = Map.of(
                    "eventId", calendarEvent.getId(),
                    "calendarId", calendarId,
                    "hasConflicts", conflictCheck.isSuccess() && !conflictCheck.getData().isEmpty(),
                    "conflictCount", conflictCheck.isSuccess() ? conflictCheck.getData().size() : 0
            );
            
            return CalendarResponse.success(calendarEvent, "Event created successfully", metadata);
            
        } catch (IOException e) {
            log.error("IO error creating event: {}", e.getMessage(), e);
            return handleCreationError(e);
        } catch (GeneralSecurityException e) {
            log.error("Security error creating event: {}", e.getMessage(), e);
            return CalendarResponse.error("Security error accessing Google Calendar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating event: {}", e.getMessage(), e);
            return CalendarResponse.error("Unexpected error creating event: " + e.getMessage());
        }
    }
    
    /**
     * Create a quick event with minimal parameters
     */
    public CalendarResponse<CalendarEvent> createQuickEvent(String title, String startTime, String endTime) {
        return createQuickEvent(title, startTime, endTime, null, null);
    }
    
    /**
     * Create a quick event with title, times, and optional description/location
     */
    public CalendarResponse<CalendarEvent> createQuickEvent(
            String title, String startTime, String endTime, String description, String location) {
        
        try {
            // Parse times
            java.time.LocalDateTime start = parseDateTime(startTime);
            java.time.LocalDateTime end = parseDateTime(endTime);
            
            if (start == null || end == null) {
                return CalendarResponse.validationError("Invalid date/time format. Use ISO format (e.g., 2024-01-15T14:30:00)");
            }
            
            // Build event request
            EventRequest eventRequest = EventRequest.builder()
                    .title(title)
                    .description(description)
                    .location(location)
                    .startTime(start)
                    .endTime(end)
                    .build();
            
            return createEvent(eventRequest);
            
        } catch (Exception e) {
            log.error("Error creating quick event: {}", e.getMessage(), e);
            return CalendarResponse.error("Error creating quick event: " + e.getMessage());
        }
    }
    
    /**
     * Create an all-day event
     */
    public CalendarResponse<CalendarEvent> createAllDayEvent(String title, String date) {
        return createAllDayEvent(title, date, null, null);
    }
    
    /**
     * Create an all-day event with optional description and location
     */
    public CalendarResponse<CalendarEvent> createAllDayEvent(
            String title, String date, String description, String location) {
        
        try {
            // Parse date
            java.time.LocalDate eventDate = java.time.LocalDate.parse(date);
            java.time.LocalDateTime startTime = eventDate.atStartOfDay();
            java.time.LocalDateTime endTime = eventDate.plusDays(1).atStartOfDay();
            
            // Build event request
            EventRequest eventRequest = EventRequest.builder()
                    .title(title)
                    .description(description)
                    .location(location)
                    .startTime(startTime)
                    .endTime(endTime)
                    .allDay(true)
                    .build();
            
            return createEvent(eventRequest);
            
        } catch (Exception e) {
            log.error("Error creating all-day event: {}", e.getMessage(), e);
            return CalendarResponse.error("Error creating all-day event: " + e.getMessage());
        }
    }
    
    /**
     * Create a recurring event (basic implementation)
     */
    public CalendarResponse<CalendarEvent> createRecurringEvent(
            EventRequest eventRequest, String recurrenceRule) {
        
        try {
            // Validate base event
            ValidationResult validation = eventValidator.validateEventRequest(eventRequest);
            if (!validation.isValid()) {
                return CalendarResponse.validationError("Event validation failed: " + 
                        String.join(", ", validation.getErrors()));
            }
            
            // Convert to Google event
            Event googleEvent = eventMapper.fromEventRequestToGoogle(eventRequest);
            if (googleEvent == null) {
                return CalendarResponse.error("Failed to convert event request");
            }
            
            // Add recurrence rule
            if (recurrenceRule != null && !recurrenceRule.trim().isEmpty()) {
                googleEvent.setRecurrence(List.of(recurrenceRule));
            }
            
            // Create event
            Calendar service = calendarClient.getCalendarService();
            String calendarId = eventRequest.getCalendarId() != null ? 
                    eventRequest.getCalendarId() : properties.getDefaultCalendarId();
            
            Event createdEvent = service.events().insert(calendarId, googleEvent).execute();
            
            CalendarEvent calendarEvent = eventMapper.fromGoogleEvent(createdEvent);
            calendarEvent.setCalendarId(calendarId);
            
            log.info("Successfully created recurring event: {} (ID: {})", 
                    calendarEvent.getTitle(), calendarEvent.getId());
            
            return CalendarResponse.eventCreated(calendarEvent);
            
        } catch (Exception e) {
            log.error("Error creating recurring event: {}", e.getMessage(), e);
            return CalendarResponse.error("Error creating recurring event: " + e.getMessage());
        }
    }
    
    /**
     * Batch create multiple events
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<List<CalendarEvent>> createMultipleEvents(List<EventRequest> eventRequests) {
        if (eventRequests == null || eventRequests.isEmpty()) {
            return CalendarResponse.validationError("No events provided for creation");
        }
        
        if (eventRequests.size() > 10) {
            return CalendarResponse.validationError("Cannot create more than 10 events at once");
        }
        
        List<CalendarEvent> createdEvents = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        
        for (int i = 0; i < eventRequests.size(); i++) {
            EventRequest request = eventRequests.get(i);
            CalendarResponse<CalendarEvent> response = createEvent(request);
            
            if (response.isSuccess()) {
                createdEvents.add(response.getData());
            } else {
                errors.add(String.format("Event %d (%s): %s", i + 1, request.getTitle(), response.getError()));
            }
        }
        
        if (createdEvents.isEmpty()) {
            return CalendarResponse.error("Failed to create any events: " + String.join("; ", errors));
        }
        
        String message = String.format("Created %d of %d events successfully", 
                createdEvents.size(), eventRequests.size());
        
        if (!errors.isEmpty()) {
            message += ". Errors: " + String.join("; ", errors);
        }
        
        return CalendarResponse.success(createdEvents, message);
    }
    
    private java.time.LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return java.time.LocalDateTime.parse(dateTimeString.trim());
        } catch (java.time.format.DateTimeParseException e) {
            log.debug("Failed to parse datetime: {}", dateTimeString);
            return null;
        }
    }
    
    private CalendarResponse<CalendarEvent> handleCreationError(IOException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("rate limit") || message.contains("quota")) {
            return CalendarResponse.rateLimitError();
        } else if (message.contains("unauthorized") || message.contains("authentication")) {
            return CalendarResponse.authenticationError();
        } else if (message.contains("forbidden") || message.contains("permission")) {
            return CalendarResponse.permissionError();
        } else if (message.contains("conflict") || message.contains("duplicate")) {
            return CalendarResponse.conflictError("Event creation conflict: " + e.getMessage());
        } else if (message.contains("network") || message.contains("connection")) {
            return CalendarResponse.networkError();
        } else {
            return CalendarResponse.error("Google Calendar API error: " + e.getMessage());
        }
    }
}