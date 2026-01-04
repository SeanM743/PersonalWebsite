package com.personal.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {
    
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private ZoneId timezone;
    private String calendarId;
    private EventStatus status;
    private EventVisibility visibility;
    private List<String> attendees;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String recurringEventId;
    private String meetingUrl;
    private List<String> tags;
    
    // Helper methods
    public boolean isAllDay() {
        return startTime != null && endTime != null && 
               startTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) &&
               endTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT);
    }
    
    public boolean isRecurring() {
        return recurringEventId != null && !recurringEventId.trim().isEmpty();
    }
    
    public boolean isUpcoming() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }
    
    public boolean isInProgress() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null && 
               startTime.isBefore(now) && endTime.isAfter(now);
    }
    
    public boolean isPast() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }
    
    public long getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }
    
    public enum EventStatus {
        CONFIRMED,
        TENTATIVE,
        CANCELLED
    }
    
    public enum EventVisibility {
        DEFAULT,
        PUBLIC,
        PRIVATE,
        CONFIDENTIAL
    }
}