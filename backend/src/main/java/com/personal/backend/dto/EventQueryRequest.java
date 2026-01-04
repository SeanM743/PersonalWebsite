package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventQueryRequest {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String calendarId;
    private String searchQuery;
    private List<String> tags;
    private Integer maxResults;
    private String orderBy;
    private boolean showDeleted;
    private boolean singleEvents;
    private String timeZone;
    
    // Default values
    public static EventQueryRequest defaultQuery() {
        return EventQueryRequest.builder()
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .maxResults(50)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
    }
    
    public static EventQueryRequest upcomingEvents() {
        return EventQueryRequest.builder()
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(7))
                .maxResults(20)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
    }
    
    public static EventQueryRequest todayEvents() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        
        return EventQueryRequest.builder()
                .startDate(startOfDay)
                .endDate(endOfDay)
                .maxResults(50)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
    }
    
    public static EventQueryRequest thisWeekEvents() {
        LocalDateTime startOfWeek = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        
        return EventQueryRequest.builder()
                .startDate(startOfWeek)
                .endDate(endOfWeek)
                .maxResults(100)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
    }
    
    // Validation methods
    public boolean isValidDateRange() {
        return startDate != null && endDate != null && startDate.isBefore(endDate);
    }
    
    public boolean hasSearchCriteria() {
        return (searchQuery != null && !searchQuery.trim().isEmpty()) ||
               (tags != null && !tags.isEmpty());
    }
}