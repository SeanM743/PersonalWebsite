package com.personal.backend.integration;

import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.model.CalendarEvent;
import com.personal.backend.service.GoogleCalendarEventCreator;
import com.personal.backend.service.GoogleCalendarEventDeleter;
import com.personal.backend.service.GoogleCalendarEventManager;
import com.personal.backend.service.GoogleCalendarEventUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CALENDAR_CREDENTIALS_PATH", matches = ".*")
class GoogleCalendarIntegrationTest {
    
    @Autowired
    private GoogleCalendarEventManager eventManager;
    
    @Autowired
    private GoogleCalendarEventCreator eventCreator;
    
    @Autowired
    private GoogleCalendarEventUpdater eventUpdater;
    
    @Autowired
    private GoogleCalendarEventDeleter eventDeleter;
    
    private EventRequest testEventRequest;
    private String createdEventId;
    
    @BeforeEach
    void setUp() {
        testEventRequest = EventRequest.builder()
                .title("Integration Test Event")
                .description("This is a test event created by integration tests")
                .location("Test Location")
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .build();
    }
    
    @Test
    void testCompleteEventLifecycle() {
        // Test event creation
        CalendarResponse<CalendarEvent> createResponse = eventCreator.createEvent(testEventRequest);
        
        assertThat(createResponse.isSuccess()).isTrue();
        assertThat(createResponse.getData()).isNotNull();
        assertThat(createResponse.getData().getTitle()).isEqualTo(testEventRequest.getTitle());
        
        createdEventId = createResponse.getData().getId();
        assertThat(createdEventId).isNotNull();
        
        try {
            // Test event retrieval
            CalendarResponse<CalendarEvent> getResponse = eventManager.getEventById(createdEventId, null);
            
            assertThat(getResponse.isSuccess()).isTrue();
            assertThat(getResponse.getData()).isNotNull();
            assertThat(getResponse.getData().getId()).isEqualTo(createdEventId);
            assertThat(getResponse.getData().getTitle()).isEqualTo(testEventRequest.getTitle());
            
            // Test event update
            EventRequest updateRequest = EventRequest.builder()
                    .title("Updated Integration Test Event")
                    .description("Updated description")
                    .location("Updated Location")
                    .startTime(testEventRequest.getStartTime())
                    .endTime(testEventRequest.getEndTime())
                    .build();
            
            CalendarResponse<CalendarEvent> updateResponse = eventUpdater.updateEvent(createdEventId, updateRequest);
            
            assertThat(updateResponse.isSuccess()).isTrue();
            assertThat(updateResponse.getData().getTitle()).isEqualTo("Updated Integration Test Event");
            assertThat(updateResponse.getData().getDescription()).isEqualTo("Updated description");
            
            // Test event search
            CalendarResponse<List<CalendarEvent>> searchResponse = eventManager.searchEvents("Updated Integration", 10);
            
            assertThat(searchResponse.isSuccess()).isTrue();
            assertThat(searchResponse.getData()).isNotEmpty();
            assertThat(searchResponse.getData().stream()
                    .anyMatch(event -> event.getId().equals(createdEventId))).isTrue();
            
        } finally {
            // Clean up - delete the test event
            if (createdEventId != null) {
                CalendarResponse<Boolean> deleteResponse = eventDeleter.deleteEvent(createdEventId);
                assertThat(deleteResponse.isSuccess()).isTrue();
            }
        }
    }
    
    @Test
    void testUpcomingEventsRetrieval() {
        CalendarResponse<List<CalendarEvent>> response = eventManager.getUpcomingEvents(7);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        // Events list can be empty, that's valid
    }
    
    @Test
    void testTodayEventsRetrieval() {
        CalendarResponse<List<CalendarEvent>> response = eventManager.getTodayEvents();
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        // Events list can be empty, that's valid
    }
    
    @Test
    void testConflictDetection() {
        // Create a test event first
        CalendarResponse<CalendarEvent> createResponse = eventCreator.createEvent(testEventRequest);
        
        if (createResponse.isSuccess()) {
            createdEventId = createResponse.getData().getId();
            
            try {
                // Test conflict detection with overlapping time
                EventRequest conflictingRequest = EventRequest.builder()
                        .title("Conflicting Event")
                        .startTime(testEventRequest.getStartTime().plusMinutes(30))
                        .endTime(testEventRequest.getEndTime().plusMinutes(30))
                        .build();
                
                CalendarResponse<List<CalendarEvent>> conflictResponse = eventManager.checkForConflicts(conflictingRequest);
                
                assertThat(conflictResponse.isSuccess()).isTrue();
                assertThat(conflictResponse.getData()).isNotEmpty();
                assertThat(conflictResponse.getData().stream()
                        .anyMatch(event -> event.getId().equals(createdEventId))).isTrue();
                
            } finally {
                // Clean up
                eventDeleter.deleteEvent(createdEventId);
            }
        }
    }
    
    @Test
    void testQuickEventCreation() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(3);
        LocalDateTime endTime = startTime.plusHours(1);
        
        CalendarResponse<CalendarEvent> response = eventCreator.createQuickEvent(
                "Quick Test Event",
                startTime.toString(),
                endTime.toString(),
                "Quick test description",
                "Quick test location"
        );
        
        if (response.isSuccess()) {
            createdEventId = response.getData().getId();
            
            assertThat(response.getData().getTitle()).isEqualTo("Quick Test Event");
            assertThat(response.getData().getDescription()).isEqualTo("Quick test description");
            
            // Clean up
            eventDeleter.deleteEvent(createdEventId);
        }
    }
    
    @Test
    void testAllDayEventCreation() {
        String tomorrow = LocalDateTime.now().plusDays(1).toLocalDate().toString();
        
        CalendarResponse<CalendarEvent> response = eventCreator.createAllDayEvent(
                "All Day Test Event",
                tomorrow,
                "All day test description",
                "All day test location"
        );
        
        if (response.isSuccess()) {
            createdEventId = response.getData().getId();
            
            assertThat(response.getData().getTitle()).isEqualTo("All Day Test Event");
            assertThat(response.getData().isAllDay()).isTrue();
            
            // Clean up
            eventDeleter.deleteEvent(createdEventId);
        }
    }
    
    @Test
    void testEventDeletion() {
        // Create event to delete
        CalendarResponse<CalendarEvent> createResponse = eventCreator.createEvent(testEventRequest);
        
        if (createResponse.isSuccess()) {
            String eventId = createResponse.getData().getId();
            
            // Delete the event
            CalendarResponse<Boolean> deleteResponse = eventDeleter.deleteEvent(eventId);
            
            assertThat(deleteResponse.isSuccess()).isTrue();
            assertThat(deleteResponse.getData()).isTrue();
            
            // Verify event is deleted
            CalendarResponse<CalendarEvent> getResponse = eventManager.getEventById(eventId, null);
            assertThat(getResponse.isSuccess()).isFalse();
            assertThat(getResponse.getError()).contains("not found");
        }
    }
    
    @Test
    void testEventsByDateRange() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        
        CalendarResponse<List<CalendarEvent>> response = eventManager.getEventsInRange(start, end);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        
        // Verify all events are within the date range
        for (CalendarEvent event : response.getData()) {
            if (event.getStartTime() != null) {
                assertThat(event.getStartTime()).isBetween(start, end);
            }
        }
    }
    
    @Test
    void testInvalidEventCreation() {
        // Test with invalid date range
        EventRequest invalidRequest = EventRequest.builder()
                .title("Invalid Event")
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(1)) // End before start
                .build();
        
        CalendarResponse<CalendarEvent> response = eventCreator.createEvent(invalidRequest);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("validation");
    }
    
    @Test
    void testEventNotFound() {
        CalendarResponse<CalendarEvent> response = eventManager.getEventById("nonexistent-event-id", null);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("not found");
    }
}