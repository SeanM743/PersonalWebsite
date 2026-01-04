package com.personal.backend.service;

import com.personal.backend.dto.EventRequest;
import com.personal.backend.util.ValidationResult;
import com.personal.backend.model.CalendarEvent;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;
import net.jqwik.time.api.DateTimes;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarPropertyTest {
    
    @Mock
    private EventValidator eventValidator;
    
    @Mock
    private EventMapper eventMapper;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Property
    @Label("Event validation should be consistent for valid events")
    void eventValidationConsistency(
            @ForAll @NotBlank String title,
            @ForAll("validDateTimes") LocalDateTime startTime,
            @ForAll("validDateTimes") LocalDateTime endTime) {
        
        Assume.that(startTime.isBefore(endTime));
        
        EventRequest eventRequest = EventRequest.builder()
                .title(title.trim())
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        EventValidator validator = new EventValidator();
        ValidationResult result1 = validator.validateEventRequest(eventRequest);
        ValidationResult result2 = validator.validateEventRequest(eventRequest);
        
        // Property: Validation should be deterministic
        assertThat(result1.isValid()).isEqualTo(result2.isValid());
        assertThat(result1.getErrors()).isEqualTo(result2.getErrors());
    }
    
    @Property
    @Label("Event start time must always be before end time")
    void eventTimeOrderingProperty(
            @ForAll @NotBlank String title,
            @ForAll("validDateTimes") LocalDateTime startTime,
            @ForAll("validDateTimes") LocalDateTime endTime) {
        
        EventRequest eventRequest = EventRequest.builder()
                .title(title.trim())
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        EventValidator validator = new EventValidator();
        ValidationResult result = validator.validateEventRequest(eventRequest);
        
        // Property: If validation passes, start time must be before end time
        if (result.isValid()) {
            assertThat(startTime).isBefore(endTime);
        }
        
        // Property: If start time is after end time, validation must fail
        if (!startTime.isBefore(endTime)) {
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotNull();
            assertThat(result.getErrors().stream().anyMatch(error -> 
                    error.toLowerCase().contains("start") && error.toLowerCase().contains("end")))
                    .isTrue();
        }
    }
    
    @Property
    @Label("Event title validation should reject empty or whitespace-only titles")
    void eventTitleValidationProperty(@ForAll String title) {
        EventRequest eventRequest = EventRequest.builder()
                .title(title)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .build();
        
        EventValidator validator = new EventValidator();
        ValidationResult result = validator.validateEventRequest(eventRequest);
        
        // Property: Empty or whitespace-only titles should be invalid
        if (title == null || title.trim().isEmpty()) {
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isNotNull();
            assertThat(result.getErrors().stream().anyMatch(error -> 
                    error.toLowerCase().contains("title")))
                    .isTrue();
        }
    }
    
    @Property
    @Label("Date range queries should return events within the specified range")
    void dateRangeQueryProperty(
            @ForAll("validDateTimes") LocalDateTime queryStart,
            @ForAll("validDateTimes") LocalDateTime queryEnd,
            @ForAll("eventList") List<CalendarEvent> allEvents) {
        
        Assume.that(queryStart.isBefore(queryEnd));
        
        // Filter events that should be in range
        List<CalendarEvent> expectedInRange = allEvents.stream()
                .filter(event -> event.getStartTime() != null && event.getEndTime() != null)
                .filter(event -> 
                        !event.getEndTime().isBefore(queryStart) && 
                        !event.getStartTime().isAfter(queryEnd))
                .toList();
        
        // Property: All events in the filtered list should overlap with the query range
        for (CalendarEvent event : expectedInRange) {
            assertThat(event.getStartTime()).isNotNull();
            assertThat(event.getEndTime()).isNotNull();
            
            // Event overlaps if: eventStart <= queryEnd && eventEnd >= queryStart
            boolean overlaps = !event.getStartTime().isAfter(queryEnd) && 
                              !event.getEndTime().isBefore(queryStart);
            assertThat(overlaps).isTrue();
        }
    }
    
    @Property
    @Label("Conflict detection should be symmetric")
    void conflictDetectionSymmetryProperty(
            @ForAll("validEventRequest") EventRequest event1,
            @ForAll("validEventRequest") EventRequest event2) {
        
        boolean conflict1to2 = hasTimeConflict(event1, event2);
        boolean conflict2to1 = hasTimeConflict(event2, event1);
        
        // Property: Conflict detection should be symmetric
        assertThat(conflict1to2).isEqualTo(conflict2to1);
    }
    
    @Property
    @Label("Event duration should always be positive for valid events")
    void eventDurationProperty(@ForAll("validEventRequest") EventRequest eventRequest) {
        if (eventRequest.getStartTime() != null && eventRequest.getEndTime() != null) {
            EventValidator validator = new EventValidator();
            ValidationResult result = validator.validateEventRequest(eventRequest);
            
            if (result.isValid()) {
                // Property: Valid events must have positive duration
                assertThat(eventRequest.getStartTime()).isBefore(eventRequest.getEndTime());
                
                long durationMinutes = java.time.Duration.between(
                        eventRequest.getStartTime(), 
                        eventRequest.getEndTime()).toMinutes();
                assertThat(durationMinutes).isPositive();
            }
        }
    }
    
    @Property
    @Label("Cache key generation should be consistent and unique")
    void cacheKeyConsistencyProperty(
            @ForAll @Positive int days,
            @ForAll @NotBlank String query,
            @ForAll @Positive int maxResults) {
        
        // Generate cache keys multiple times
        String key1 = generateCacheKey("upcoming", days);
        String key2 = generateCacheKey("upcoming", days);
        String searchKey1 = generateSearchCacheKey(query, maxResults);
        String searchKey2 = generateSearchCacheKey(query, maxResults);
        
        // Property: Same parameters should generate same cache key
        assertThat(key1).isEqualTo(key2);
        assertThat(searchKey1).isEqualTo(searchKey2);
        
        // Property: Different parameters should generate different cache keys
        if (days > 1) {
            String differentKey = generateCacheKey("upcoming", days - 1);
            assertThat(key1).isNotEqualTo(differentKey);
        }
    }
    
    @Property
    @Label("Error handling should preserve error information")
    void errorHandlingProperty(@ForAll("errorScenarios") Exception exception) {
        GoogleCalendarErrorHandler errorHandler = new GoogleCalendarErrorHandler();
        
        var response = errorHandler.handleApiError(exception, "test-operation");
        
        // Property: Error responses should always contain error information
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError()).isNotBlank();
        
        // Property: Retryable errors should be identified correctly
        boolean isRetryable = errorHandler.isRetryableError(exception);
        String userMessage = errorHandler.getUserFriendlyMessage(exception, "test-operation");
        
        assertThat(userMessage).isNotNull();
        assertThat(userMessage).isNotBlank();
    }
    
    // Arbitraries for property testing
    
    @Provide
    Arbitrary<LocalDateTime> validDateTimes() {
        return DateTimes.dateTimes()
                .between(
                        LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().plusYears(1)
                )
                .filter(dt -> dt.isAfter(LocalDateTime.now().minusDays(1)));
    }
    
    @Provide
    Arbitrary<EventRequest> validEventRequest() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
                validDateTimes(),
                validDateTimes()
        ).as((title, start, end) -> {
            LocalDateTime startTime = start;
            LocalDateTime endTime = end;
            
            // Ensure end is after start
            if (!startTime.isBefore(endTime)) {
                endTime = startTime.plusHours(1);
            }
            
            return EventRequest.builder()
                    .title(title)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        });
    }
    
    @Provide
    Arbitrary<List<CalendarEvent>> eventList() {
        return validEventRequest()
                .map(this::createCalendarEvent)
                .list()
                .ofMaxSize(20);
    }
    
    @Provide
    Arbitrary<Exception> errorScenarios() {
        return Arbitraries.oneOf(
                Arbitraries.just(new java.io.IOException("Network error")),
                Arbitraries.just(new java.net.SocketTimeoutException("Timeout")),
                Arbitraries.just(new java.net.UnknownHostException("Unknown host")),
                Arbitraries.just(new java.security.GeneralSecurityException("Security error")),
                Arbitraries.just(new RuntimeException("Generic error"))
        );
    }
    
    // Helper methods
    
    private boolean hasTimeConflict(EventRequest event1, EventRequest event2) {
        if (event1.getStartTime() == null || event1.getEndTime() == null ||
            event2.getStartTime() == null || event2.getEndTime() == null) {
            return false;
        }
        
        return event1.getStartTime().isBefore(event2.getEndTime()) &&
               event2.getStartTime().isBefore(event1.getEndTime());
    }
    
    private String generateCacheKey(String type, int days) {
        return type + ":" + days;
    }
    
    private String generateSearchCacheKey(String query, int maxResults) {
        return "search:" + query + ":" + maxResults;
    }
    
    private CalendarEvent createCalendarEvent(EventRequest request) {
        CalendarEvent event = new CalendarEvent();
        event.setId("test-" + System.nanoTime());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }
}