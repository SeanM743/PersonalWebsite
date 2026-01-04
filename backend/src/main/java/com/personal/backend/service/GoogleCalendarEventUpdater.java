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
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarEventUpdater {
    
    private final GoogleCalendarClient calendarClient;
    private final GoogleCalendarEventManager eventManager;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;
    private final GoogleCalendarConfiguration.GoogleCalendarProperties properties;
    
    /**
     * Update an existing calendar event
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> updateEvent(String eventId, EventRequest eventRequest) {
        return updateEvent(eventId, eventRequest, null);
    }
    
    /**
     * Update an existing calendar event with specific calendar ID
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> updateEvent(String eventId, EventRequest eventRequest, String calendarId) {
        try {
            log.info("Updating calendar event: {} ({})", eventRequest.getTitle(), eventId);
            
            // Validate service availability
            if (!calendarClient.isServiceAvailable()) {
                return CalendarResponse.error("Google Calendar service is not available. Please check your credentials.");
            }
            
            // Validate inputs
            if (eventId == null || eventId.trim().isEmpty()) {
                return CalendarResponse.validationError("Event ID is required for update");
            }
            
            ValidationResult validation = eventValidator.validateEventRequest(eventRequest);
            if (!validation.isValid()) {
                String errorMessage = "Event validation failed: " + String.join(", ", validation.getErrors());
                log.warn("Event update validation failed: {}", errorMessage);
                return CalendarResponse.validationError(errorMessage);
            }
            
            String effectiveCalendarId = calendarId != null ? calendarId : properties.getDefaultCalendarId();
            Calendar service = calendarClient.getCalendarService();
            
            // First, check if event exists
            CalendarResponse<CalendarEvent> existingEventResponse = eventManager.getEventById(eventId, effectiveCalendarId);
            if (!existingEventResponse.isSuccess()) {
                if (existingEventResponse.getError().contains("not found")) {
                    return CalendarResponse.eventNotFound();
                }
                return existingEventResponse;
            }
            
            CalendarEvent existingEvent = existingEventResponse.getData();
            
            // Check for concurrent modifications by comparing last modified time
            if (eventRequest.getLastModified() != null && existingEvent.getUpdatedAt() != null) {
                if (existingEvent.getUpdatedAt().isAfter(eventRequest.getLastModified())) {
                    return CalendarResponse.conflictError("Event has been modified by another user. Please refresh and try again.");
                }
            }
            
            // Convert updated request to Google Calendar event
            Event googleEvent = eventMapper.fromEventRequestToGoogle(eventRequest);
            if (googleEvent == null) {
                return CalendarResponse.error("Failed to convert event request to Google Calendar format");
            }
            
            // Set the event ID for update
            googleEvent.setId(eventId);
            
            // Execute update
            Event updatedEvent = service.events().update(effectiveCalendarId, eventId, googleEvent).execute();
            
            if (updatedEvent == null) {
                return CalendarResponse.error("Event update failed - no response from Google Calendar");
            }
            
            // Convert back to internal model
            CalendarEvent calendarEvent = eventMapper.fromGoogleEvent(updatedEvent);
            if (calendarEvent == null) {
                return CalendarResponse.error("Failed to convert updated event data");
            }
            
            calendarEvent.setCalendarId(effectiveCalendarId);
            
            log.info("Successfully updated calendar event: {} (ID: {})", 
                    calendarEvent.getTitle(), calendarEvent.getId());
            
            Map<String, Object> metadata = Map.of(
                    "eventId", calendarEvent.getId(),
                    "calendarId", effectiveCalendarId,
                    "previousTitle", existingEvent.getTitle(),
                    "updatedAt", calendarEvent.getUpdatedAt() != null ? calendarEvent.getUpdatedAt().toString() : "unknown"
            );
            
            return CalendarResponse.success(calendarEvent, "Event updated successfully", metadata);
            
        } catch (IOException e) {
            log.error("IO error updating event {}: {}", eventId, e.getMessage(), e);
            return handleUpdateError(e);
        } catch (GeneralSecurityException e) {
            log.error("Security error updating event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Security error accessing Google Calendar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Unexpected error updating event: " + e.getMessage());
        }
    }
    
    /**
     * Partially update an event (patch operation)
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> patchEvent(String eventId, Map<String, Object> updates) {
        return patchEvent(eventId, updates, null);
    }
    
    /**
     * Partially update an event with specific calendar ID
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> patchEvent(String eventId, Map<String, Object> updates, String calendarId) {
        try {
            log.info("Patching calendar event: {} with {} updates", eventId, updates.size());
            
            if (eventId == null || eventId.trim().isEmpty()) {
                return CalendarResponse.validationError("Event ID is required for patch");
            }
            
            if (updates == null || updates.isEmpty()) {
                return CalendarResponse.validationError("No updates provided");
            }
            
            String effectiveCalendarId = calendarId != null ? calendarId : properties.getDefaultCalendarId();
            
            // Get existing event
            CalendarResponse<CalendarEvent> existingEventResponse = eventManager.getEventById(eventId, effectiveCalendarId);
            if (!existingEventResponse.isSuccess()) {
                return existingEventResponse;
            }
            
            CalendarEvent existingEvent = existingEventResponse.getData();
            
            // Build updated event request from existing event + updates
            EventRequest.EventRequestBuilder builder = EventRequest.builder()
                    .title(existingEvent.getTitle())
                    .description(existingEvent.getDescription())
                    .location(existingEvent.getLocation())
                    .startTime(existingEvent.getStartTime())
                    .endTime(existingEvent.getEndTime())
                    .allDay(existingEvent.isAllDay())
                    .calendarId(effectiveCalendarId);
            
            // Apply updates
            if (updates.containsKey("title")) {
                builder.title((String) updates.get("title"));
            }
            if (updates.containsKey("description")) {
                builder.description((String) updates.get("description"));
            }
            if (updates.containsKey("location")) {
                builder.location((String) updates.get("location"));
            }
            if (updates.containsKey("startTime")) {
                Object startTime = updates.get("startTime");
                if (startTime instanceof String) {
                    builder.startTime(LocalDateTime.parse((String) startTime));
                } else if (startTime instanceof LocalDateTime) {
                    builder.startTime((LocalDateTime) startTime);
                }
            }
            if (updates.containsKey("endTime")) {
                Object endTime = updates.get("endTime");
                if (endTime instanceof String) {
                    builder.endTime(LocalDateTime.parse((String) endTime));
                } else if (endTime instanceof LocalDateTime) {
                    builder.endTime((LocalDateTime) endTime);
                }
            }
            if (updates.containsKey("allDay")) {
                builder.allDay((Boolean) updates.get("allDay"));
            }
            
            EventRequest updatedRequest = builder.build();
            
            return updateEvent(eventId, updatedRequest, effectiveCalendarId);
            
        } catch (Exception e) {
            log.error("Error patching event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Error patching event: " + e.getMessage());
        }
    }
    
    /**
     * Move an event to a different time
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> moveEvent(String eventId, LocalDateTime newStartTime, LocalDateTime newEndTime) {
        return moveEvent(eventId, newStartTime, newEndTime, null);
    }
    
    /**
     * Move an event to a different time with specific calendar ID
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> moveEvent(String eventId, LocalDateTime newStartTime, LocalDateTime newEndTime, String calendarId) {
        if (newStartTime == null || newEndTime == null) {
            return CalendarResponse.validationError("Both start and end times are required for moving an event");
        }
        
        if (!newStartTime.isBefore(newEndTime)) {
            return CalendarResponse.validationError("Start time must be before end time");
        }
        
        Map<String, Object> updates = Map.of(
                "startTime", newStartTime,
                "endTime", newEndTime
        );
        
        return patchEvent(eventId, updates, calendarId);
    }
    
    /**
     * Update event title only
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> updateEventTitle(String eventId, String newTitle) {
        return updateEventTitle(eventId, newTitle, null);
    }
    
    /**
     * Update event title only with specific calendar ID
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<CalendarEvent> updateEventTitle(String eventId, String newTitle, String calendarId) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return CalendarResponse.validationError("Event title cannot be empty");
        }
        
        Map<String, Object> updates = Map.of("title", newTitle.trim());
        return patchEvent(eventId, updates, calendarId);
    }
    
    private CalendarResponse<CalendarEvent> handleUpdateError(IOException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("404") || message.contains("not found")) {
            return CalendarResponse.eventNotFound();
        } else if (message.contains("409") || message.contains("conflict")) {
            return CalendarResponse.conflictError("Event update conflict: " + e.getMessage());
        } else if (message.contains("rate limit") || message.contains("quota")) {
            return CalendarResponse.rateLimitError();
        } else if (message.contains("unauthorized") || message.contains("authentication")) {
            return CalendarResponse.authenticationError();
        } else if (message.contains("forbidden") || message.contains("permission")) {
            return CalendarResponse.permissionError();
        } else if (message.contains("network") || message.contains("connection")) {
            return CalendarResponse.networkError();
        } else {
            return CalendarResponse.error("Google Calendar API error: " + e.getMessage());
        }
    }
}