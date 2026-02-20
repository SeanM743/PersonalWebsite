package com.personal.backend.controller;

import com.personal.backend.dto.CalendarResponse;
import com.personal.backend.dto.EventQueryRequest;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.model.CalendarEvent;
import com.personal.backend.service.GoogleCalendarClient;
import com.personal.backend.service.GoogleCalendarEventCreator;
import com.personal.backend.service.GoogleCalendarEventDeleter;
import com.personal.backend.service.GoogleCalendarEventManager;
import com.personal.backend.service.GoogleCalendarEventUpdater;
import com.personal.backend.service.GoogleCalendarTokenManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CalendarController {
    
    private final GoogleCalendarEventManager eventManager;
    private final GoogleCalendarEventCreator eventCreator;
    private final GoogleCalendarEventUpdater eventUpdater;
    private final GoogleCalendarEventDeleter eventDeleter;
    private final GoogleCalendarClient calendarClient;
    private final GoogleCalendarTokenManager tokenManager;
    
    /**
     * Get events - general purpose endpoint for frontend calendar page
     */
    @GetMapping("/events")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> getEvents(
            @RequestParam(defaultValue = "90") int days) {
        return getUpcomingEvents(days);
    }
    
    /**
     * Get upcoming events (public endpoint)
     */
    @GetMapping("/upcoming")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> getUpcomingEvents(
            @RequestParam(defaultValue = "7") int days) {
        
        try {
            log.info("Retrieving upcoming events for {} days", days);
            
            if (days < 1 || days > 365) {
                CalendarResponse<List<CalendarEvent>> errorResponse = 
                        CalendarResponse.validationError("Days must be between 1 and 365");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.getUpcomingEvents(days);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving upcoming events: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to retrieve upcoming events");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get today's events (public endpoint)
     */
    @GetMapping("/today")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> getTodayEvents() {
        try {
            log.info("Retrieving today's events");
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.getTodayEvents();
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving today's events: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to retrieve today's events");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get this week's events (public endpoint)
     */
    @GetMapping("/week")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> getThisWeekEvents() {
        try {
            log.info("Retrieving this week's events");
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.getThisWeekEvents();
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving week's events: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to retrieve week's events");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Search events (public endpoint)
     */
    @GetMapping("/search")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int maxResults) {
        
        try {
            log.info("Searching events with query: {}", query);
            
            if (query == null || query.trim().isEmpty()) {
                CalendarResponse<List<CalendarEvent>> errorResponse = 
                        CalendarResponse.validationError("Search query is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (maxResults < 1 || maxResults > 100) {
                CalendarResponse<List<CalendarEvent>> errorResponse = 
                        CalendarResponse.validationError("Max results must be between 1 and 100");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.searchEvents(query, maxResults);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error searching events: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to search events");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get events in date range (public endpoint)
     */
    @GetMapping("/range")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> getEventsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            log.info("Retrieving events in range: {} to {}", start, end);
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.getEventsInRange(start, end);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving events in range: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to retrieve events in range");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get specific event by ID (public endpoint)
     */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<CalendarResponse<CalendarEvent>> getEventById(
            @PathVariable String eventId,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Retrieving event by ID: {}", eventId);
            
            CalendarResponse<CalendarEvent> response = eventManager.getEventById(eventId, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving event by ID: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to retrieve event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create new event (admin only)
     */
    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> createEvent(
            @Valid @RequestBody EventRequest eventRequest) {
        
        try {
            log.info("Creating new event: {}", eventRequest.getTitle());
            
            CalendarResponse<CalendarEvent> response = eventCreator.createEvent(eventRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error creating event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to create event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create quick event (admin only)
     */
    @PostMapping("/events/quick")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> createQuickEvent(
            @RequestParam String title,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String location) {
        
        try {
            log.info("Creating quick event: {}", title);
            
            CalendarResponse<CalendarEvent> response = eventCreator.createQuickEvent(
                    title, startTime, endTime, description, location);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error creating quick event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to create quick event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create all-day event (admin only)
     */
    @PostMapping("/events/all-day")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> createAllDayEvent(
            @RequestParam String title,
            @RequestParam String date,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String location) {
        
        try {
            log.info("Creating all-day event: {}", title);
            
            CalendarResponse<CalendarEvent> response = eventCreator.createAllDayEvent(
                    title, date, description, location);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error creating all-day event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to create all-day event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Check for scheduling conflicts (admin only)
     */
    @PostMapping("/events/check-conflicts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<List<CalendarEvent>>> checkConflicts(
            @Valid @RequestBody EventRequest eventRequest) {
        
        try {
            log.info("Checking conflicts for event: {}", eventRequest.getTitle());
            
            CalendarResponse<List<CalendarEvent>> response = eventManager.checkForConflicts(eventRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking conflicts: {}", e.getMessage(), e);
            CalendarResponse<List<CalendarEvent>> errorResponse = 
                    CalendarResponse.error("Failed to check conflicts");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update existing event (admin only)
     */
    @PutMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> updateEvent(
            @PathVariable String eventId,
            @Valid @RequestBody EventRequest eventRequest,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Updating event: {} ({})", eventRequest.getTitle(), eventId);
            
            CalendarResponse<CalendarEvent> response = eventUpdater.updateEvent(eventId, eventRequest, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (response.getError() != null && response.getError().contains("conflict")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error updating event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to update event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Partially update event (admin only)
     */
    @PatchMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> patchEvent(
            @PathVariable String eventId,
            @RequestBody Map<String, Object> updates,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Patching event: {} with {} updates", eventId, updates.size());
            
            CalendarResponse<CalendarEvent> response = eventUpdater.patchEvent(eventId, updates, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (response.getError() != null && response.getError().contains("conflict")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error patching event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to patch event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Move event to different time (admin only)
     */
    @PutMapping("/events/{eventId}/move")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<CalendarEvent>> moveEvent(
            @PathVariable String eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Moving event: {} to {} - {}", eventId, startTime, endTime);
            
            CalendarResponse<CalendarEvent> response = eventUpdater.moveEvent(eventId, startTime, endTime, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                HttpStatus status = response.getError().contains("validation") ? 
                        HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error moving event: {}", e.getMessage(), e);
            CalendarResponse<CalendarEvent> errorResponse = 
                    CalendarResponse.error("Failed to move event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete event (admin only)
     */
    @DeleteMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<Boolean>> deleteEvent(
            @PathVariable String eventId,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Deleting event: {}", eventId);
            
            CalendarResponse<Boolean> response = eventDeleter.deleteEvent(eventId, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error deleting event: {}", e.getMessage(), e);
            CalendarResponse<Boolean> errorResponse = 
                    CalendarResponse.error("Failed to delete event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete event by title and date (admin only)
     */
    @DeleteMapping("/events/by-title")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendarResponse<Boolean>> deleteEventByTitle(
            @RequestParam String title,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(required = false) String calendarId) {
        
        try {
            log.info("Deleting event by title: {} on {}", title, date);
            
            CalendarResponse<Boolean> response = eventDeleter.deleteEventByTitleAndDate(title, date, calendarId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getError() != null && response.getError().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error deleting event by title: {}", e.getMessage(), e);
            CalendarResponse<Boolean> errorResponse = 
                    CalendarResponse.error("Failed to delete event");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            GoogleCalendarClient.HealthCheckResult healthResult = calendarClient.performHealthCheck();
            GoogleCalendarClient.ServiceInfo serviceInfo = calendarClient.getServiceInfo();
            GoogleCalendarTokenManager.TokenStatistics tokenStats = tokenManager.getTokenStatistics();
            
            Map<String, Object> health = Map.of(
                    "healthy", healthResult.isHealthy(),
                    "message", healthResult.getMessage(),
                    "details", healthResult.getDetails(),
                    "serviceAvailable", serviceInfo.isServiceAvailable(),
                    "credentialsAvailable", serviceInfo.isCredentialsAvailable(),
                    "tokenValid", tokenStats.isTokenValid(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            HttpStatus status = healthResult.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            Map<String, Object> health = Map.of(
                    "healthy", false,
                    "message", "Health check failed",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    /**
     * Service information endpoint (admin only)
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        try {
            GoogleCalendarClient.ServiceInfo serviceInfo = calendarClient.getServiceInfo();
            GoogleCalendarTokenManager.TokenStatistics tokenStats = tokenManager.getTokenStatistics();
            
            Map<String, Object> info = Map.of(
                    "serviceInfo", serviceInfo,
                    "tokenStatistics", tokenStats,
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            log.error("Error getting service info: {}", e.getMessage(), e);
            Map<String, Object> error = Map.of(
                    "error", "Failed to retrieve service information",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CalendarResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid request: {}", e.getMessage());
        CalendarResponse<Object> errorResponse = CalendarResponse.validationError(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CalendarResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected error in calendar controller: {}", e.getMessage(), e);
        CalendarResponse<Object> errorResponse = CalendarResponse.error("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}