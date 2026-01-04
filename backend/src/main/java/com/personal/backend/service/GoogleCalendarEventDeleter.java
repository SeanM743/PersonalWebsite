package com.personal.backend.service;

import com.google.api.services.calendar.Calendar;
import com.personal.backend.config.GoogleCalendarConfiguration;
import com.personal.backend.dto.CalendarResponse;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarEventDeleter {
    
    private final GoogleCalendarClient calendarClient;
    private final GoogleCalendarEventManager eventManager;
    private final GoogleCalendarConfiguration.GoogleCalendarProperties properties;
    
    /**
     * Delete an event by ID
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Boolean> deleteEvent(String eventId) {
        return deleteEvent(eventId, null);
    }
    
    /**
     * Delete an event by ID with specific calendar ID
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Boolean> deleteEvent(String eventId, String calendarId) {
        try {
            log.info("Deleting calendar event: {}", eventId);
            
            // Validate service availability
            if (!calendarClient.isServiceAvailable()) {
                return CalendarResponse.error("Google Calendar service is not available. Please check your credentials.");
            }
            
            // Validate input
            if (eventId == null || eventId.trim().isEmpty()) {
                return CalendarResponse.validationError("Event ID is required for deletion");
            }
            
            String effectiveCalendarId = calendarId != null ? calendarId : properties.getDefaultCalendarId();
            
            // First, check if event exists and get its details for logging
            CalendarResponse<CalendarEvent> existingEventResponse = eventManager.getEventById(eventId, effectiveCalendarId);
            String eventTitle = "Unknown";
            if (existingEventResponse.isSuccess()) {
                eventTitle = existingEventResponse.getData().getTitle();
            } else if (existingEventResponse.getError().contains("not found")) {
                return CalendarResponse.eventNotFound();
            }
            
            // Execute deletion
            Calendar service = calendarClient.getCalendarService();
            service.events().delete(effectiveCalendarId, eventId).execute();
            
            log.info("Successfully deleted calendar event: {} (ID: {})", eventTitle, eventId);
            
            Map<String, Object> metadata = Map.of(
                    "eventId", eventId,
                    "calendarId", effectiveCalendarId,
                    "deletedTitle", eventTitle,
                    "deletedAt", LocalDateTime.now().toString()
            );
            
            return CalendarResponse.success(true, "Event deleted successfully", metadata);
            
        } catch (IOException e) {
            log.error("IO error deleting event {}: {}", eventId, e.getMessage(), e);
            return handleDeletionError(e);
        } catch (GeneralSecurityException e) {
            log.error("Security error deleting event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Security error accessing Google Calendar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Unexpected error deleting event: " + e.getMessage());
        }
    }
    
    /**
     * Delete event by title and date (for cases where ID is not known)
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Boolean> deleteEventByTitleAndDate(String title, LocalDateTime eventDate) {
        return deleteEventByTitleAndDate(title, eventDate, null);
    }
    
    /**
     * Delete event by title and date with specific calendar ID
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Boolean> deleteEventByTitleAndDate(String title, LocalDateTime eventDate, String calendarId) {
        try {
            log.info("Deleting event by title and date: {} on {}", title, eventDate);
            
            if (title == null || title.trim().isEmpty()) {
                return CalendarResponse.validationError("Event title is required for deletion by title");
            }
            
            if (eventDate == null) {
                return CalendarResponse.validationError("Event date is required for deletion by title");
            }
            
            // Search for events matching the title and date
            LocalDateTime startOfDay = eventDate.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = eventDate.toLocalDate().atTime(23, 59, 59);
            
            CalendarResponse<List<CalendarEvent>> eventsResponse = eventManager.getEventsInRange(startOfDay, endOfDay);
            
            if (!eventsResponse.isSuccess()) {
                return CalendarResponse.error("Failed to search for events: " + eventsResponse.getError());
            }
            
            List<CalendarEvent> matchingEvents = eventsResponse.getData().stream()
                    .filter(event -> title.equalsIgnoreCase(event.getTitle()))
                    .toList();
            
            if (matchingEvents.isEmpty()) {
                return CalendarResponse.eventNotFound();
            }
            
            if (matchingEvents.size() > 1) {
                return CalendarResponse.error(String.format(
                        "Multiple events found with title '%s' on %s. Please use event ID for deletion.", 
                        title, eventDate.toLocalDate()));
            }
            
            CalendarEvent eventToDelete = matchingEvents.get(0);
            return deleteEvent(eventToDelete.getId(), calendarId);
            
        } catch (Exception e) {
            log.error("Error deleting event by title and date: {}", e.getMessage(), e);
            return CalendarResponse.error("Error deleting event: " + e.getMessage());
        }
    }
    
    /**
     * Delete multiple events by IDs
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Map<String, Boolean>> deleteMultipleEvents(List<String> eventIds) {
        return deleteMultipleEvents(eventIds, null);
    }
    
    /**
     * Delete multiple events by IDs with specific calendar ID
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Map<String, Boolean>> deleteMultipleEvents(List<String> eventIds, String calendarId) {
        if (eventIds == null || eventIds.isEmpty()) {
            return CalendarResponse.validationError("No event IDs provided for deletion");
        }
        
        if (eventIds.size() > 20) {
            return CalendarResponse.validationError("Cannot delete more than 20 events at once");
        }
        
        Map<String, Boolean> results = new java.util.HashMap<>();
        List<String> errors = new java.util.ArrayList<>();
        int successCount = 0;
        
        for (String eventId : eventIds) {
            CalendarResponse<Boolean> response = deleteEvent(eventId, calendarId);
            
            if (response.isSuccess()) {
                results.put(eventId, true);
                successCount++;
            } else {
                results.put(eventId, false);
                errors.add(String.format("Event %s: %s", eventId, response.getError()));
            }
        }
        
        String message = String.format("Deleted %d of %d events successfully", successCount, eventIds.size());
        
        if (!errors.isEmpty()) {
            message += ". Errors: " + String.join("; ", errors);
        }
        
        Map<String, Object> metadata = Map.of(
                "totalRequested", eventIds.size(),
                "successCount", successCount,
                "failureCount", eventIds.size() - successCount,
                "errors", errors
        );
        
        if (successCount == 0) {
            return CalendarResponse.error("Failed to delete any events: " + String.join("; ", errors));
        }
        
        return CalendarResponse.success(results, message, metadata);
    }
    
    /**
     * Delete all events in a date range (use with caution)
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Integer> deleteEventsInRange(LocalDateTime startDate, LocalDateTime endDate) {
        return deleteEventsInRange(startDate, endDate, null);
    }
    
    /**
     * Delete all events in a date range with specific calendar ID (use with caution)
     */
    @CacheEvict(value = {"upcomingEvents", "todayEvents", "weekEvents"}, allEntries = true)
    public CalendarResponse<Integer> deleteEventsInRange(LocalDateTime startDate, LocalDateTime endDate, String calendarId) {
        try {
            log.warn("Deleting all events in range: {} to {}", startDate, endDate);
            
            if (startDate == null || endDate == null) {
                return CalendarResponse.validationError("Both start and end dates are required");
            }
            
            if (!startDate.isBefore(endDate)) {
                return CalendarResponse.validationError("Start date must be before end date");
            }
            
            // Safety check: prevent deletion of more than 30 days of events
            if (startDate.plusDays(30).isBefore(endDate)) {
                return CalendarResponse.validationError("Cannot delete events spanning more than 30 days at once");
            }
            
            // Get all events in the range
            CalendarResponse<List<CalendarEvent>> eventsResponse = eventManager.getEventsInRange(startDate, endDate);
            
            if (!eventsResponse.isSuccess()) {
                return CalendarResponse.error("Failed to retrieve events for deletion: " + eventsResponse.getError());
            }
            
            List<CalendarEvent> eventsToDelete = eventsResponse.getData();
            
            if (eventsToDelete.isEmpty()) {
                return CalendarResponse.success(0, "No events found in the specified range");
            }
            
            // Delete events one by one
            int deletedCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (CalendarEvent event : eventsToDelete) {
                CalendarResponse<Boolean> deleteResponse = deleteEvent(event.getId(), calendarId);
                
                if (deleteResponse.isSuccess()) {
                    deletedCount++;
                } else {
                    errors.add(String.format("Event %s (%s): %s", 
                            event.getId(), event.getTitle(), deleteResponse.getError()));
                }
            }
            
            String message = String.format("Deleted %d of %d events in range", deletedCount, eventsToDelete.size());
            
            if (!errors.isEmpty()) {
                message += ". Errors: " + String.join("; ", errors);
            }
            
            Map<String, Object> metadata = Map.of(
                    "dateRange", String.format("%s to %s", startDate, endDate),
                    "totalFound", eventsToDelete.size(),
                    "deletedCount", deletedCount,
                    "failureCount", eventsToDelete.size() - deletedCount,
                    "errors", errors
            );
            
            return CalendarResponse.success(deletedCount, message, metadata);
            
        } catch (Exception e) {
            log.error("Error deleting events in range: {}", e.getMessage(), e);
            return CalendarResponse.error("Error deleting events in range: " + e.getMessage());
        }
    }
    
    private CalendarResponse<Boolean> handleDeletionError(IOException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("404") || message.contains("not found")) {
            return CalendarResponse.eventNotFound();
        } else if (message.contains("410") || message.contains("gone")) {
            return CalendarResponse.success(true, "Event was already deleted");
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