package com.personal.backend.dto;

import com.personal.backend.model.CalendarEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EventRequest {
    
    @NotBlank(message = "Event title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    @Size(max = 500, message = "Location must be less than 500 characters")
    private String location;
    
    private ZoneId timezone;
    
    private String calendarId;
    
    private CalendarEvent.EventStatus status;
    
    private CalendarEvent.EventVisibility visibility;
    
    private List<String> attendees;
    
    private String meetingUrl;
    
    private List<String> tags;
    
    private boolean allDay;
    
    private LocalDateTime lastModified;
    
    // Alias method for backward compatibility
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    // Validation helper methods
    public boolean isValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
    
    public boolean isInFuture() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }
    
    public long getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }
}