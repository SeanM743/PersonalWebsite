package com.personal.backend.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.personal.backend.config.GoogleCalendarConfiguration;
import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.dto.EventQueryRequest;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.model.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarEventManager {
    
    private final GoogleCalendarClient calendarClient;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;
    private final GoogleCalendarConfiguration.GoogleCalendarProperties properties;
    
    /**
     * Retrieve events based on query parameters
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CalendarResponse<List<CalendarEvent>> getEvents(EventQueryRequest query) {
        try {
            log.debug("Retrieving events with query: {}", query);
            
            if (!calendarClient.isServiceAvailable()) {
                return CalendarResponse.error("Google Calendar service is not available. Please check your credentials.");
            }
            
            // Validate query
            if (query != null && !query.isValidDateRange()) {
                return CalendarResponse.validationError("Invalid date range in query");
            }
            
            // Use default query if none provided
            if (query == null) {
                query = EventQueryRequest.defaultQuery();
            }
            
            Calendar service = calendarClient.getCalendarService();
            String calendarId = query.getCalendarId() != null ? query.getCalendarId() : properties.getDefaultCalendarId();
            
            // Build the API request
            Calendar.Events.List request = service.events().list(calendarId);
            
            // Set query parameters
            configureEventQuery(request, query);
            
            // Execute the request
            Events events = request.execute();
            
            if (events.getItems() == null || events.getItems().isEmpty()) {
                log.debug("No events found for query");
                return CalendarResponse.noEventsFound();
            }
            
            // Convert Google events to internal model
            List<CalendarEvent> calendarEvents = events.getItems().stream()
                    .map(eventMapper::fromGoogleEvent)
                    .filter(event -> event != null)
                    .collect(Collectors.toList());
            
            // Apply additional filtering if needed
            calendarEvents = applyAdditionalFiltering(calendarEvents, query);
            
            // Sort events
            sortEvents(calendarEvents, query.getOrderBy());
            
            log.info("Retrieved {} events from Google Calendar", calendarEvents.size());
            
            Map<String, Object> metadata = Map.of(
                    "totalEvents", calendarEvents.size(),
                    "calendarId", calendarId,
                    "queryRange", String.format("%s to %s", query.getStartDate(), query.getEndDate())
            );
            
            return CalendarResponse.success(calendarEvents, "Events retrieved successfully", metadata);
            
        } catch (IOException e) {
            log.error("IO error retrieving events: {}", e.getMessage(), e);
            return handleApiError(e);
        } catch (GeneralSecurityException e) {
            log.error("Security error retrieving events: {}", e.getMessage(), e);
            return CalendarResponse.error("Security error accessing Google Calendar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving events: {}", e.getMessage(), e);
            return CalendarResponse.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Get upcoming events (cached for performance)
     */
    @Cacheable(value = "upcomingEvents", key = "#days")
    public CalendarResponse<List<CalendarEvent>> getUpcomingEvents(int days) {
        EventQueryRequest query = EventQueryRequest.builder()
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(days))
                .maxResults(50)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
        
        return getEvents(query);
    }
    
    /**
     * Get today's events (cached for performance)
     */
    @Cacheable(value = "todayEvents")
    public CalendarResponse<List<CalendarEvent>> getTodayEvents() {
        return getEvents(EventQueryRequest.todayEvents());
    }
    
    /**
     * Get this week's events (cached for performance)
     */
    @Cacheable(value = "weekEvents")
    public CalendarResponse<List<CalendarEvent>> getThisWeekEvents() {
        return getEvents(EventQueryRequest.thisWeekEvents());
    }
    
    /**
     * Search events by text query
     */
    public CalendarResponse<List<CalendarEvent>> searchEvents(String searchQuery, int maxResults) {
        EventQueryRequest query = EventQueryRequest.builder()
                .searchQuery(searchQuery)
                .startDate(LocalDateTime.now().minusMonths(1))
                .endDate(LocalDateTime.now().plusMonths(3))
                .maxResults(maxResults > 0 ? maxResults : 20)
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
        
        return getEvents(query);
    }
    
    /**
     * Get events in a specific date range
     */
    public CalendarResponse<List<CalendarEvent>> getEventsInRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return CalendarResponse.validationError("Invalid date range provided");
        }
        
        EventQueryRequest query = EventQueryRequest.builder()
                .startDate(start)
                .endDate(end)
                .maxResults(properties.getMaxEventsPerRequest())
                .orderBy("startTime")
                .showDeleted(false)
                .singleEvents(true)
                .build();
        
        return getEvents(query);
    }
    
    /**
     * Get a single event by ID
     */
    @Retryable(
        retryFor = {IOException.class, GeneralSecurityException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public CalendarResponse<CalendarEvent> getEventById(String eventId, String calendarId) {
        try {
            if (eventId == null || eventId.trim().isEmpty()) {
                return CalendarResponse.validationError("Event ID is required");
            }
            
            if (!calendarClient.isServiceAvailable()) {
                return CalendarResponse.error("Google Calendar service is not available");
            }
            
            Calendar service = calendarClient.getCalendarService();
            String effectiveCalendarId = calendarId != null ? calendarId : properties.getDefaultCalendarId();
            
            Event googleEvent = service.events().get(effectiveCalendarId, eventId).execute();
            
            if (googleEvent == null) {
                return CalendarResponse.eventNotFound();
            }
            
            CalendarEvent calendarEvent = eventMapper.fromGoogleEvent(googleEvent);
            if (calendarEvent == null) {
                return CalendarResponse.error("Failed to convert event data");
            }
            
            calendarEvent.setCalendarId(effectiveCalendarId);
            
            log.debug("Retrieved event: {} ({})", calendarEvent.getTitle(), eventId);
            return CalendarResponse.success(calendarEvent);
            
        } catch (IOException e) {
            if (e.getMessage().contains("404") || e.getMessage().contains("Not Found")) {
                return CalendarResponse.eventNotFound();
            }
            log.error("IO error retrieving event {}: {}", eventId, e.getMessage());
            return CalendarResponse.error("Google Calendar API error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving event {}: {}", eventId, e.getMessage(), e);
            return CalendarResponse.error("Error retrieving event: " + e.getMessage());
        }
    }
    
    /**
     * Check for scheduling conflicts
     */
    public CalendarResponse<List<CalendarEvent>> checkForConflicts(EventRequest eventRequest) {
        if (eventRequest == null || eventRequest.getStartTime() == null || eventRequest.getEndTime() == null) {
            return CalendarResponse.validationError("Event start and end times are required for conflict checking");
        }
        
        // Get events in the same time range
        CalendarResponse<List<CalendarEvent>> eventsResponse = getEventsInRange(
                eventRequest.getStartTime().minusHours(1),
                eventRequest.getEndTime().plusHours(1)
        );
        
        if (!eventsResponse.isSuccess()) {
            return CalendarResponse.error(eventsResponse.getMessage());
        }
        
        List<CalendarEvent> conflictingEvents = eventsResponse.getData().stream()
                .filter(event -> isTimeConflict(eventRequest, event))
                .collect(Collectors.toList());
        
        if (conflictingEvents.isEmpty()) {
            return CalendarResponse.success(Collections.emptyList(), "No scheduling conflicts found");
        } else {
            return CalendarResponse.success(conflictingEvents, 
                    String.format("Found %d potential scheduling conflicts", conflictingEvents.size()));
        }
    }
    
    private void configureEventQuery(Calendar.Events.List request, EventQueryRequest query) throws IOException {
        // Set time range
        if (query.getStartDate() != null) {
            DateTime startTime = new DateTime(
                    ZonedDateTime.of(query.getStartDate(), ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
            request.setTimeMin(startTime);
        }
        
        if (query.getEndDate() != null) {
            DateTime endTime = new DateTime(
                    ZonedDateTime.of(query.getEndDate(), ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
            request.setTimeMax(endTime);
        }
        
        // Set other parameters
        if (query.getMaxResults() != null && query.getMaxResults() > 0) {
            request.setMaxResults(Math.min(query.getMaxResults(), properties.getMaxEventsPerRequest()));
        } else {
            request.setMaxResults(properties.getMaxEventsPerRequest());
        }
        
        if (query.getSearchQuery() != null && !query.getSearchQuery().trim().isEmpty()) {
            request.setQ(query.getSearchQuery());
        }
        
        if (query.getOrderBy() != null) {
            request.setOrderBy(query.getOrderBy());
        }
        
        request.setShowDeleted(query.isShowDeleted());
        request.setSingleEvents(query.isSingleEvents());
        
        if (query.getTimeZone() != null) {
            request.setTimeZone(query.getTimeZone());
        }
    }
    
    private List<CalendarEvent> applyAdditionalFiltering(List<CalendarEvent> events, EventQueryRequest query) {
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            // Filter by tags if specified
            events = events.stream()
                    .filter(event -> event.getTags() != null && 
                                   event.getTags().stream().anyMatch(tag -> query.getTags().contains(tag)))
                    .collect(Collectors.toList());
        }
        
        return events;
    }
    
    private void sortEvents(List<CalendarEvent> events, String orderBy) {
        if (orderBy == null || orderBy.equals("startTime")) {
            events.sort((a, b) -> {
                if (a.getStartTime() == null && b.getStartTime() == null) return 0;
                if (a.getStartTime() == null) return 1;
                if (b.getStartTime() == null) return -1;
                return a.getStartTime().compareTo(b.getStartTime());
            });
        } else if (orderBy.equals("updated")) {
            events.sort((a, b) -> {
                if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                if (a.getUpdatedAt() == null) return 1;
                if (b.getUpdatedAt() == null) return -1;
                return b.getUpdatedAt().compareTo(a.getUpdatedAt()); // Descending for updated
            });
        }
    }
    
    private boolean isTimeConflict(EventRequest newEvent, CalendarEvent existingEvent) {
        if (newEvent.getStartTime() == null || newEvent.getEndTime() == null ||
            existingEvent.getStartTime() == null || existingEvent.getEndTime() == null) {
            return false;
        }
        
        // Events conflict if: newStart < existingEnd && existingStart < newEnd
        return newEvent.getStartTime().isBefore(existingEvent.getEndTime()) &&
               existingEvent.getStartTime().isBefore(newEvent.getEndTime());
    }
    
    private CalendarResponse<List<CalendarEvent>> handleApiError(IOException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("rate limit") || message.contains("quota")) {
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