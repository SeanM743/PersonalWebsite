package com.personal.backend.config;

import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.model.CalendarEvent;
import com.personal.backend.service.GoogleCalendarEventCreator;
import com.personal.backend.service.GoogleCalendarEventDeleter;
import com.personal.backend.service.GoogleCalendarEventManager;
import com.personal.backend.service.GoogleCalendarEventUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CalendarFunctionConfiguration {
    
    private final GoogleCalendarEventManager eventManager;
    private final GoogleCalendarEventCreator eventCreator;
    private final GoogleCalendarEventUpdater eventUpdater;
    private final GoogleCalendarEventDeleter eventDeleter;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Bean
    @Description("Get upcoming calendar events for the next few days")
    public Function<GetUpcomingEventsRequest, String> getUpcomingEvents() {
        return request -> {
            try {
                log.info("AI function called: getUpcomingEvents with days: {}", request.days());
                
                int days = request.days() > 0 ? Math.min(request.days(), 30) : 7;
                
                CalendarResponse<List<CalendarEvent>> response = eventManager.getUpcomingEvents(days);
                
                if (response.isSuccess()) {
                    List<CalendarEvent> events = response.getData();
                    
                    if (events.isEmpty()) {
                        return String.format("No upcoming events found for the next %d days.", days);
                    }
                    
                    StringBuilder result = new StringBuilder();
                    result.append(String.format("Upcoming events for the next %d days:\n", days));
                    
                    for (CalendarEvent event : events) {
                        result.append(formatEventForAI(event)).append("\n");
                    }
                    
                    return result.toString();
                } else {
                    return "Unable to retrieve upcoming events: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getUpcomingEvents", e);
                return "Error retrieving upcoming events: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get today's calendar events")
    public Function<GetTodayEventsRequest, String> getTodayEvents() {
        return request -> {
            try {
                log.info("AI function called: getTodayEvents");
                
                CalendarResponse<List<CalendarEvent>> response = eventManager.getTodayEvents();
                
                if (response.isSuccess()) {
                    List<CalendarEvent> events = response.getData();
                    
                    if (events.isEmpty()) {
                        return "No events scheduled for today.";
                    }
                    
                    StringBuilder result = new StringBuilder("Today's events:\n");
                    
                    for (CalendarEvent event : events) {
                        result.append(formatEventForAI(event)).append("\n");
                    }
                    
                    return result.toString();
                } else {
                    return "Unable to retrieve today's events: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getTodayEvents", e);
                return "Error retrieving today's events: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Search calendar events by title, description, or location")
    public Function<SearchEventsRequest, String> searchEvents() {
        return request -> {
            try {
                log.info("AI function called: searchEvents with query: {}", request.query());
                
                if (request.query() == null || request.query().trim().isEmpty()) {
                    return "Search query cannot be empty.";
                }
                
                int maxResults = request.maxResults() > 0 ? Math.min(request.maxResults(), 20) : 10;
                
                CalendarResponse<List<CalendarEvent>> response = eventManager.searchEvents(request.query(), maxResults);
                
                if (response.isSuccess()) {
                    List<CalendarEvent> events = response.getData();
                    
                    if (events.isEmpty()) {
                        return "No events found matching: " + request.query();
                    }
                    
                    StringBuilder result = new StringBuilder();
                    result.append(String.format("Events matching '%s':\n", request.query()));
                    
                    for (CalendarEvent event : events) {
                        result.append(formatEventForAI(event)).append("\n");
                    }
                    
                    return result.toString();
                } else {
                    return "Unable to search events: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function searchEvents", e);
                return "Error searching events: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Create a new calendar event with title, start time, end time, and optional description")
    public Function<CreateEventRequest, String> createEvent() {
        return request -> {
            try {
                log.info("AI function called: createEvent with title: {}", request.title());
                
                if (request.title() == null || request.title().trim().isEmpty()) {
                    return "Event title is required.";
                }
                
                if (request.startTime() == null || request.startTime().trim().isEmpty()) {
                    return "Event start time is required.";
                }
                
                if (request.endTime() == null || request.endTime().trim().isEmpty()) {
                    return "Event end time is required.";
                }
                
                // Parse date/time
                LocalDateTime startDateTime = parseDateTime(request.startTime());
                LocalDateTime endDateTime = parseDateTime(request.endTime());
                
                if (startDateTime == null) {
                    return "Invalid start time format. Use format: YYYY-MM-DD HH:MM (e.g., 2024-01-15 14:30)";
                }
                
                if (endDateTime == null) {
                    return "Invalid end time format. Use format: YYYY-MM-DD HH:MM (e.g., 2024-01-15 15:30)";
                }
                
                if (!startDateTime.isBefore(endDateTime)) {
                    return "Start time must be before end time.";
                }
                
                // Build event request
                EventRequest eventRequest = EventRequest.builder()
                        .title(request.title())
                        .description(request.description())
                        .location(request.location())
                        .startTime(startDateTime)
                        .endTime(endDateTime)
                        .build();
                
                CalendarResponse<CalendarEvent> response = eventCreator.createEvent(eventRequest);
                
                if (response.isSuccess()) {
                    CalendarEvent event = response.getData();
                    return String.format("Successfully created event: %s\nScheduled for: %s to %s\nEvent ID: %s", 
                            event.getTitle(),
                            formatDateTime(event.getStartTime()),
                            formatDateTime(event.getEndTime()),
                            event.getId());
                } else {
                    return "Failed to create event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createEvent", e);
                return "Error creating event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Create a quick event with just title and time (assumes 1 hour duration)")
    public Function<CreateQuickEventRequest, String> createQuickEvent() {
        return request -> {
            try {
                log.info("AI function called: createQuickEvent with title: {}", request.title());
                
                if (request.title() == null || request.title().trim().isEmpty()) {
                    return "Event title is required.";
                }
                
                if (request.startTime() == null || request.startTime().trim().isEmpty()) {
                    return "Event start time is required.";
                }
                
                // Parse start time
                LocalDateTime startDateTime = parseDateTime(request.startTime());
                if (startDateTime == null) {
                    return "Invalid start time format. Use format: YYYY-MM-DD HH:MM (e.g., 2024-01-15 14:30)";
                }
                
                // Default to 1 hour duration
                LocalDateTime endDateTime = startDateTime.plusHours(1);
                
                CalendarResponse<CalendarEvent> response = eventCreator.createQuickEvent(
                        request.title(),
                        startDateTime.toString(),
                        endDateTime.toString(),
                        request.description(),
                        request.location()
                );
                
                if (response.isSuccess()) {
                    CalendarEvent event = response.getData();
                    return String.format("Successfully created quick event: %s\nScheduled for: %s to %s", 
                            event.getTitle(),
                            formatDateTime(event.getStartTime()),
                            formatDateTime(event.getEndTime()));
                } else {
                    return "Failed to create quick event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createQuickEvent", e);
                return "Error creating quick event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Create an all-day event for a specific date")
    public Function<CreateAllDayEventRequest, String> createAllDayEvent() {
        return request -> {
            try {
                log.info("AI function called: createAllDayEvent with title: {}", request.title());
                
                if (request.title() == null || request.title().trim().isEmpty()) {
                    return "Event title is required.";
                }
                
                if (request.date() == null || request.date().trim().isEmpty()) {
                    return "Event date is required.";
                }
                
                CalendarResponse<CalendarEvent> response = eventCreator.createAllDayEvent(
                        request.title(),
                        request.date(),
                        request.description(),
                        request.location()
                );
                
                if (response.isSuccess()) {
                    CalendarEvent event = response.getData();
                    return String.format("Successfully created all-day event: %s\nDate: %s", 
                            event.getTitle(),
                            event.getStartTime().toLocalDate());
                } else {
                    return "Failed to create all-day event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createAllDayEvent", e);
                return "Error creating all-day event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Check for scheduling conflicts with a proposed event time")
    public Function<CheckConflictsRequest, String> checkConflicts() {
        return request -> {
            try {
                log.info("AI function called: checkConflicts");
                
                if (request.startTime() == null || request.endTime() == null) {
                    return "Both start time and end time are required for conflict checking.";
                }
                
                LocalDateTime startDateTime = parseDateTime(request.startTime());
                LocalDateTime endDateTime = parseDateTime(request.endTime());
                
                if (startDateTime == null || endDateTime == null) {
                    return "Invalid time format. Use format: YYYY-MM-DD HH:MM";
                }
                
                EventRequest eventRequest = EventRequest.builder()
                        .title("Conflict Check")
                        .startTime(startDateTime)
                        .endTime(endDateTime)
                        .build();
                
                CalendarResponse<List<CalendarEvent>> response = eventManager.checkForConflicts(eventRequest);
                
                if (response.isSuccess()) {
                    List<CalendarEvent> conflicts = response.getData();
                    
                    if (conflicts.isEmpty()) {
                        return String.format("No scheduling conflicts found for %s to %s", 
                                formatDateTime(startDateTime), formatDateTime(endDateTime));
                    } else {
                        StringBuilder result = new StringBuilder();
                        result.append(String.format("Found %d scheduling conflicts:\n", conflicts.size()));
                        
                        for (CalendarEvent conflict : conflicts) {
                            result.append(formatEventForAI(conflict)).append("\n");
                        }
                        
                        return result.toString();
                    }
                } else {
                    return "Unable to check for conflicts: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function checkConflicts", e);
                return "Error checking conflicts: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Update an existing calendar event by ID")
    public Function<UpdateEventRequest, String> updateEvent() {
        return request -> {
            try {
                log.info("AI function called: updateEvent with ID: {}", request.eventId());
                
                if (request.eventId() == null || request.eventId().trim().isEmpty()) {
                    return "Event ID is required for updating.";
                }
                
                if (request.title() == null || request.title().trim().isEmpty()) {
                    return "Event title is required.";
                }
                
                // Parse date/time if provided
                LocalDateTime startDateTime = null;
                LocalDateTime endDateTime = null;
                
                if (request.startTime() != null && !request.startTime().trim().isEmpty()) {
                    startDateTime = parseDateTime(request.startTime());
                    if (startDateTime == null) {
                        return "Invalid start time format. Use format: YYYY-MM-DD HH:MM";
                    }
                }
                
                if (request.endTime() != null && !request.endTime().trim().isEmpty()) {
                    endDateTime = parseDateTime(request.endTime());
                    if (endDateTime == null) {
                        return "Invalid end time format. Use format: YYYY-MM-DD HH:MM";
                    }
                }
                
                if (startDateTime != null && endDateTime != null && !startDateTime.isBefore(endDateTime)) {
                    return "Start time must be before end time.";
                }
                
                // Build event request
                EventRequest eventRequest = EventRequest.builder()
                        .title(request.title())
                        .description(request.description())
                        .location(request.location())
                        .startTime(startDateTime)
                        .endTime(endDateTime)
                        .build();
                
                CalendarResponse<CalendarEvent> response = eventUpdater.updateEvent(request.eventId(), eventRequest);
                
                if (response.isSuccess()) {
                    CalendarEvent event = response.getData();
                    return String.format("Successfully updated event: %s\nScheduled for: %s to %s\nEvent ID: %s", 
                            event.getTitle(),
                            formatDateTime(event.getStartTime()),
                            formatDateTime(event.getEndTime()),
                            event.getId());
                } else {
                    return "Failed to update event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function updateEvent", e);
                return "Error updating event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Delete a calendar event by ID")
    public Function<DeleteEventRequest, String> deleteEvent() {
        return request -> {
            try {
                log.info("AI function called: deleteEvent with ID: {}", request.eventId());
                
                if (request.eventId() == null || request.eventId().trim().isEmpty()) {
                    return "Event ID is required for deletion.";
                }
                
                CalendarResponse<Boolean> response = eventDeleter.deleteEvent(request.eventId());
                
                if (response.isSuccess()) {
                    return String.format("Successfully deleted event with ID: %s", request.eventId());
                } else {
                    return "Failed to delete event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function deleteEvent", e);
                return "Error deleting event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Delete a calendar event by title and date")
    public Function<DeleteEventByTitleRequest, String> deleteEventByTitle() {
        return request -> {
            try {
                log.info("AI function called: deleteEventByTitle with title: {}", request.title());
                
                if (request.title() == null || request.title().trim().isEmpty()) {
                    return "Event title is required for deletion.";
                }
                
                if (request.date() == null || request.date().trim().isEmpty()) {
                    return "Event date is required for deletion.";
                }
                
                LocalDateTime eventDate = parseDateTime(request.date());
                if (eventDate == null) {
                    return "Invalid date format. Use format: YYYY-MM-DD HH:MM";
                }
                
                CalendarResponse<Boolean> response = eventDeleter.deleteEventByTitleAndDate(request.title(), eventDate);
                
                if (response.isSuccess()) {
                    return String.format("Successfully deleted event: %s on %s", request.title(), request.date());
                } else {
                    return "Failed to delete event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function deleteEventByTitle", e);
                return "Error deleting event: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Move an event to a different time")
    public Function<MoveEventRequest, String> moveEvent() {
        return request -> {
            try {
                log.info("AI function called: moveEvent with ID: {}", request.eventId());
                
                if (request.eventId() == null || request.eventId().trim().isEmpty()) {
                    return "Event ID is required for moving.";
                }
                
                if (request.newStartTime() == null || request.newStartTime().trim().isEmpty()) {
                    return "New start time is required.";
                }
                
                if (request.newEndTime() == null || request.newEndTime().trim().isEmpty()) {
                    return "New end time is required.";
                }
                
                LocalDateTime startDateTime = parseDateTime(request.newStartTime());
                LocalDateTime endDateTime = parseDateTime(request.newEndTime());
                
                if (startDateTime == null || endDateTime == null) {
                    return "Invalid time format. Use format: YYYY-MM-DD HH:MM";
                }
                
                if (!startDateTime.isBefore(endDateTime)) {
                    return "Start time must be before end time.";
                }
                
                CalendarResponse<CalendarEvent> response = eventUpdater.moveEvent(request.eventId(), startDateTime, endDateTime);
                
                if (response.isSuccess()) {
                    CalendarEvent event = response.getData();
                    return String.format("Successfully moved event: %s\nNew time: %s to %s", 
                            event.getTitle(),
                            formatDateTime(event.getStartTime()),
                            formatDateTime(event.getEndTime()));
                } else {
                    return "Failed to move event: " + response.getError();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function moveEvent", e);
                return "Error moving event: " + e.getMessage();
            }
        };
    }
    
    private String formatEventForAI(CalendarEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(event.getTitle());
        
        if (event.getStartTime() != null) {
            if (event.isAllDay()) {
                sb.append(" (All day on ").append(event.getStartTime().toLocalDate()).append(")");
            } else {
                sb.append(" (").append(formatDateTime(event.getStartTime()));
                if (event.getEndTime() != null) {
                    sb.append(" - ").append(formatDateTime(event.getEndTime()));
                }
                sb.append(")");
            }
        }
        
        if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
            sb.append(" at ").append(event.getLocation());
        }
        
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            String desc = event.getDescription();
            if (desc.length() > 100) {
                desc = desc.substring(0, 100) + "...";
            }
            sb.append(" - ").append(desc);
        }
        
        return sb.toString();
    }
    
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateTimeString.trim();
        
        try {
            // Try ISO format first
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException e1) {
            try {
                // Try custom format
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                log.debug("Failed to parse datetime: {}", trimmed);
                return null;
            }
        }
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    // Request record classes for AI function parameters
    public record GetUpcomingEventsRequest(int days) {}
    public record GetTodayEventsRequest() {}
    public record SearchEventsRequest(String query, int maxResults) {}
    public record CreateEventRequest(String title, String startTime, String endTime, String description, String location) {}
    public record CreateQuickEventRequest(String title, String startTime, String description, String location) {}
    public record CreateAllDayEventRequest(String title, String date, String description, String location) {}
    public record CheckConflictsRequest(String startTime, String endTime) {}
    public record UpdateEventRequest(String eventId, String title, String startTime, String endTime, String description, String location) {}
    public record DeleteEventRequest(String eventId) {}
    public record DeleteEventByTitleRequest(String title, String date) {}
    public record MoveEventRequest(String eventId, String newStartTime, String newEndTime) {}
}