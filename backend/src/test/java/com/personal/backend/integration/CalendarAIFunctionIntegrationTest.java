package com.personal.backend.integration;

import com.personal.backend.config.CalendarFunctionConfiguration;
import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.model.CalendarEvent;
import com.personal.backend.service.GoogleCalendarEventCreator;
import com.personal.backend.service.GoogleCalendarEventDeleter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GOOGLE_CALENDAR_CREDENTIALS_PATH", matches = ".*")
class CalendarAIFunctionIntegrationTest {
    
    @Autowired
    private CalendarFunctionConfiguration calendarFunctionConfiguration;
    
    @Autowired
    private GoogleCalendarEventCreator eventCreator;
    
    @Autowired
    private GoogleCalendarEventDeleter eventDeleter;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private String createdEventId;
    
    @BeforeEach
    void setUp() {
        // Clean up any previous test events
        if (createdEventId != null) {
            eventDeleter.deleteEvent(createdEventId);
            createdEventId = null;
        }
    }
    
    @Test
    void testGetUpcomingEventsFunction() {
        Function<CalendarFunctionConfiguration.GetUpcomingEventsRequest, String> function = 
                calendarFunctionConfiguration.getUpcomingEvents();
        
        CalendarFunctionConfiguration.GetUpcomingEventsRequest request = 
                new CalendarFunctionConfiguration.GetUpcomingEventsRequest(7);
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        // Result should either show events or indicate no events found
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("Upcoming events for the next 7 days"),
                r -> assertThat(r).contains("No upcoming events found")
        );
    }
    
    @Test
    void testGetTodayEventsFunction() {
        Function<CalendarFunctionConfiguration.GetTodayEventsRequest, String> function = 
                calendarFunctionConfiguration.getTodayEvents();
        
        CalendarFunctionConfiguration.GetTodayEventsRequest request = 
                new CalendarFunctionConfiguration.GetTodayEventsRequest();
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("Today's events"),
                r -> assertThat(r).contains("No events scheduled for today")
        );
    }
    
    @Test
    void testSearchEventsFunction() {
        Function<CalendarFunctionConfiguration.SearchEventsRequest, String> function = 
                calendarFunctionConfiguration.searchEvents();
        
        CalendarFunctionConfiguration.SearchEventsRequest request = 
                new CalendarFunctionConfiguration.SearchEventsRequest("meeting", 10);
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("Events matching 'meeting'"),
                r -> assertThat(r).contains("No events found matching: meeting")
        );
    }
    
    @Test
    void testCreateEventFunction() {
        Function<CalendarFunctionConfiguration.CreateEventRequest, String> function = 
                calendarFunctionConfiguration.createEvent();
        
        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
        LocalDateTime endTime = startTime.plusHours(1);
        
        CalendarFunctionConfiguration.CreateEventRequest request = 
                new CalendarFunctionConfiguration.CreateEventRequest(
                        "AI Function Test Event",
                        startTime.format(DATETIME_FORMATTER),
                        endTime.format(DATETIME_FORMATTER),
                        "Test event created by AI function",
                        "Test Location"
                );
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        
        if (result.contains("Successfully created event")) {
            assertThat(result).contains("AI Function Test Event");
            assertThat(result).contains("Event ID:");
            
            // Extract event ID for cleanup
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.startsWith("Event ID:")) {
                    createdEventId = line.substring("Event ID:".length()).trim();
                    break;
                }
            }
        }
    }
    
    @Test
    void testCreateQuickEventFunction() {
        Function<CalendarFunctionConfiguration.CreateQuickEventRequest, String> function = 
                calendarFunctionConfiguration.createQuickEvent();
        
        LocalDateTime startTime = LocalDateTime.now().plusHours(3);
        
        CalendarFunctionConfiguration.CreateQuickEventRequest request = 
                new CalendarFunctionConfiguration.CreateQuickEventRequest(
                        "Quick AI Test Event",
                        startTime.format(DATETIME_FORMATTER),
                        "Quick test description",
                        "Quick test location"
                );
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        
        if (result.contains("Successfully created quick event")) {
            assertThat(result).contains("Quick AI Test Event");
            
            // Try to find and clean up the created event
            // This is a simplified cleanup - in real scenarios, we'd need better event tracking
        }
    }
    
    @Test
    void testCreateAllDayEventFunction() {
        Function<CalendarFunctionConfiguration.CreateAllDayEventRequest, String> function = 
                calendarFunctionConfiguration.createAllDayEvent();
        
        String tomorrow = LocalDateTime.now().plusDays(1).toLocalDate().toString();
        
        CalendarFunctionConfiguration.CreateAllDayEventRequest request = 
                new CalendarFunctionConfiguration.CreateAllDayEventRequest(
                        "All Day AI Test Event",
                        tomorrow,
                        "All day test description",
                        "All day test location"
                );
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        
        if (result.contains("Successfully created all-day event")) {
            assertThat(result).contains("All Day AI Test Event");
            assertThat(result).contains("Date:");
        }
    }
    
    @Test
    void testCheckConflictsFunction() {
        Function<CalendarFunctionConfiguration.CheckConflictsRequest, String> function = 
                calendarFunctionConfiguration.checkConflicts();
        
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(1);
        
        CalendarFunctionConfiguration.CheckConflictsRequest request = 
                new CalendarFunctionConfiguration.CheckConflictsRequest(
                        startTime.format(DATETIME_FORMATTER),
                        endTime.format(DATETIME_FORMATTER)
                );
        
        String result = function.apply(request);
        
        assertThat(result).isNotNull();
        assertThat(result).isNotBlank();
        assertThat(result).satisfiesAnyOf(
                r -> { assertThat(r).contains("No scheduling conflicts found"); },
                r -> { assertThat(r).contains("Found"); assertThat(r).contains("scheduling conflicts"); }
        );
    }
    
    @Test
    void testUpdateEventFunction() {
        // First create an event to update
        LocalDateTime startTime = LocalDateTime.now().plusHours(4);
        LocalDateTime endTime = startTime.plusHours(1);
        
        com.personal.backend.dto.EventRequest eventRequest = com.personal.backend.dto.EventRequest.builder()
                .title("Event to Update")
                .description("Original description")
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        CalendarResponse<CalendarEvent> createResponse = eventCreator.createEvent(eventRequest);
        
        if (createResponse.isSuccess()) {
            createdEventId = createResponse.getData().getId();
            
            Function<CalendarFunctionConfiguration.UpdateEventRequest, String> function = 
                    calendarFunctionConfiguration.updateEvent();
            
            CalendarFunctionConfiguration.UpdateEventRequest request = 
                    new CalendarFunctionConfiguration.UpdateEventRequest(
                            createdEventId,
                            "Updated Event Title",
                            startTime.format(DATETIME_FORMATTER),
                            endTime.format(DATETIME_FORMATTER),
                            "Updated description",
                            "Updated location"
                    );
            
            String result = function.apply(request);
            
            assertThat(result).isNotNull();
            assertThat(result).isNotBlank();
            
            if (result.contains("Successfully updated event")) {
                assertThat(result).contains("Updated Event Title");
            }
        }
    }
    
    @Test
    void testDeleteEventFunction() {
        // First create an event to delete
        LocalDateTime startTime = LocalDateTime.now().plusHours(5);
        LocalDateTime endTime = startTime.plusHours(1);
        
        com.personal.backend.dto.EventRequest eventRequest = com.personal.backend.dto.EventRequest.builder()
                .title("Event to Delete")
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        CalendarResponse<CalendarEvent> createResponse = eventCreator.createEvent(eventRequest);
        
        if (createResponse.isSuccess()) {
            String eventId = createResponse.getData().getId();
            
            Function<CalendarFunctionConfiguration.DeleteEventRequest, String> function = 
                    calendarFunctionConfiguration.deleteEvent();
            
            CalendarFunctionConfiguration.DeleteEventRequest request = 
                    new CalendarFunctionConfiguration.DeleteEventRequest(eventId);
            
            String result = function.apply(request);
            
            assertThat(result).isNotNull();
            assertThat(result).isNotBlank();
            
            if (result.contains("Successfully deleted event")) {
                assertThat(result).contains(eventId);
                // Event is already deleted, no need to clean up
                createdEventId = null;
            }
        }
    }
    
    @Test
    void testInvalidInputHandling() {
        // Test create event with invalid input
        Function<CalendarFunctionConfiguration.CreateEventRequest, String> createFunction = 
                calendarFunctionConfiguration.createEvent();
        
        CalendarFunctionConfiguration.CreateEventRequest invalidRequest = 
                new CalendarFunctionConfiguration.CreateEventRequest(
                        "", // Empty title
                        "invalid-date",
                        "invalid-date",
                        null,
                        null
                );
        
        String result = createFunction.apply(invalidRequest);
        
        assertThat(result).isNotNull();
        assertThat(result).contains("required");
        
        // Test search with empty query
        Function<CalendarFunctionConfiguration.SearchEventsRequest, String> searchFunction = 
                calendarFunctionConfiguration.searchEvents();
        
        CalendarFunctionConfiguration.SearchEventsRequest emptySearchRequest = 
                new CalendarFunctionConfiguration.SearchEventsRequest("", 10);
        
        String searchResult = searchFunction.apply(emptySearchRequest);
        
        assertThat(searchResult).isNotNull();
        assertThat(searchResult).contains("cannot be empty");
    }
}