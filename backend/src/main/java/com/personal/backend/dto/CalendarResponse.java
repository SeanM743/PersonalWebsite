package com.personal.backend.dto;

import com.personal.backend.model.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse<T> {
    
    private T data;
    private boolean success;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    // Static factory methods for common responses
    public static <T> CalendarResponse<T> success(T data) {
        return CalendarResponse.<T>builder()
                .data(data)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> CalendarResponse<T> success(T data, String message) {
        return CalendarResponse.<T>builder()
                .data(data)
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> CalendarResponse<T> success(T data, String message, Map<String, Object> metadata) {
        return CalendarResponse.<T>builder()
                .data(data)
                .success(true)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> CalendarResponse<T> error(String error) {
        return CalendarResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> CalendarResponse<T> error(String error, Map<String, Object> metadata) {
        return CalendarResponse.<T>builder()
                .success(false)
                .error(error)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // Specialized factory methods for calendar operations
    public static CalendarResponse<CalendarEvent> eventCreated(CalendarEvent event) {
        return success(event, "Event created successfully");
    }
    
    public static CalendarResponse<CalendarEvent> eventUpdated(CalendarEvent event) {
        return success(event, "Event updated successfully");
    }
    
    public static CalendarResponse<Void> eventDeleted() {
        return success(null, "Event deleted successfully");
    }
    
    public static CalendarResponse<List<CalendarEvent>> eventsRetrieved(List<CalendarEvent> events) {
        Map<String, Object> metadata = Map.of(
                "count", events.size(),
                "hasEvents", !events.isEmpty()
        );
        return success(events, "Events retrieved successfully", metadata);
    }
    
    public static CalendarResponse<List<CalendarEvent>> noEventsFound() {
        return success(List.of(), "No events found for the specified criteria");
    }
    
    // Error factory methods for common calendar errors
    public static <T> CalendarResponse<T> authenticationError() {
        return error("Authentication failed. Please check your Google Calendar credentials.");
    }
    
    public static <T> CalendarResponse<T> permissionError() {
        return error("Permission denied. Please ensure the service account has calendar access.");
    }
    
    public static <T> CalendarResponse<T> rateLimitError() {
        return error("Rate limit exceeded. Please try again later.");
    }
    
    public static <T> CalendarResponse<T> networkError() {
        return error("Network error occurred while connecting to Google Calendar API.");
    }
    
    public static <T> CalendarResponse<T> eventNotFound() {
        return error("Event not found or has been deleted.");
    }
    
    public static <T> CalendarResponse<T> validationError(String details) {
        return error("Validation failed: " + details);
    }
    
    public static <T> CalendarResponse<T> conflictError(String details) {
        return error("Scheduling conflict: " + details);
    }
    
    public static <T> CalendarResponse<T> serviceUnavailableError() {
        return error("Google Calendar service is temporarily unavailable. Please try again later.");
    }
    
    public static <T> CalendarResponse<T> timeoutError() {
        return error("Request timed out while connecting to Google Calendar API.");
    }
}