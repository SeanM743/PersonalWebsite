package com.personal.backend.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.personal.backend.dto.EventRequest;
import com.personal.backend.model.CalendarEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventMapper {
    
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Convert Google Calendar API Event to internal CalendarEvent model
     */
    public CalendarEvent fromGoogleEvent(Event googleEvent) {
        if (googleEvent == null) {
            return null;
        }
        
        try {
            CalendarEvent.CalendarEventBuilder builder = CalendarEvent.builder()
                    .id(googleEvent.getId())
                    .title(googleEvent.getSummary())
                    .description(googleEvent.getDescription())
                    .location(googleEvent.getLocation())
                    .calendarId(extractCalendarId(googleEvent))
                    .status(mapEventStatus(googleEvent.getStatus()))
                    .visibility(mapEventVisibility(googleEvent.getVisibility()))
                    .createdBy(extractCreator(googleEvent))
                    .recurringEventId(googleEvent.getRecurringEventId())
                    .meetingUrl(extractMeetingUrl(googleEvent));
            
            // Map start and end times
            mapEventTimes(googleEvent, builder);
            
            // Map attendees
            if (googleEvent.getAttendees() != null) {
                List<String> attendees = googleEvent.getAttendees().stream()
                        .map(attendee -> attendee.getEmail())
                        .filter(email -> email != null)
                        .collect(Collectors.toList());
                builder.attendees(attendees);
            }
            
            // Map timestamps
            if (googleEvent.getCreated() != null) {
                builder.createdAt(convertDateTime(googleEvent.getCreated()));
            }
            
            if (googleEvent.getUpdated() != null) {
                builder.updatedAt(convertDateTime(googleEvent.getUpdated()));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error mapping Google Event to CalendarEvent: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert internal CalendarEvent model to Google Calendar API Event
     */
    public Event toGoogleEvent(CalendarEvent calendarEvent) {
        if (calendarEvent == null) {
            return null;
        }
        
        try {
            Event googleEvent = new Event()
                    .setSummary(calendarEvent.getTitle())
                    .setDescription(calendarEvent.getDescription())
                    .setLocation(calendarEvent.getLocation())
                    .setStatus(mapToGoogleStatus(calendarEvent.getStatus()))
                    .setVisibility(mapToGoogleVisibility(calendarEvent.getVisibility()));
            
            // Set start and end times
            if (calendarEvent.getStartTime() != null) {
                googleEvent.setStart(createEventDateTime(calendarEvent.getStartTime(), calendarEvent.getTimezone()));
            }
            
            if (calendarEvent.getEndTime() != null) {
                googleEvent.setEnd(createEventDateTime(calendarEvent.getEndTime(), calendarEvent.getTimezone()));
            }
            
            // Set attendees
            if (calendarEvent.getAttendees() != null && !calendarEvent.getAttendees().isEmpty()) {
                List<com.google.api.services.calendar.model.EventAttendee> attendees = 
                        calendarEvent.getAttendees().stream()
                                .map(email -> new com.google.api.services.calendar.model.EventAttendee().setEmail(email))
                                .collect(Collectors.toList());
                googleEvent.setAttendees(attendees);
            }
            
            return googleEvent;
            
        } catch (Exception e) {
            log.error("Error mapping CalendarEvent to Google Event: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert EventRequest DTO to internal CalendarEvent model
     */
    public CalendarEvent fromEventRequest(EventRequest request) {
        if (request == null) {
            return null;
        }
        
        return CalendarEvent.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .timezone(request.getTimezone() != null ? request.getTimezone() : DEFAULT_TIMEZONE)
                .calendarId(request.getCalendarId())
                .status(request.getStatus() != null ? request.getStatus() : CalendarEvent.EventStatus.CONFIRMED)
                .visibility(request.getVisibility() != null ? request.getVisibility() : CalendarEvent.EventVisibility.DEFAULT)
                .attendees(request.getAttendees())
                .meetingUrl(request.getMeetingUrl())
                .tags(request.getTags())
                .build();
    }
    
    /**
     * Convert EventRequest DTO directly to Google Calendar API Event
     */
    public Event fromEventRequestToGoogle(EventRequest request) {
        CalendarEvent calendarEvent = fromEventRequest(request);
        return toGoogleEvent(calendarEvent);
    }
    
    private void mapEventTimes(Event googleEvent, CalendarEvent.CalendarEventBuilder builder) {
        // Extract timezone first so we can use it for time conversion
        ZoneId eventTimezone = DEFAULT_TIMEZONE;
        if (googleEvent.getStart() != null) {
            eventTimezone = extractTimezone(googleEvent.getStart());
        }
        builder.timezone(eventTimezone);
        
        // Handle start time — convert using the event's own timezone
        if (googleEvent.getStart() != null) {
            LocalDateTime startTime = convertEventDateTime(googleEvent.getStart(), eventTimezone);
            builder.startTime(startTime);
        }
        
        // Handle end time
        if (googleEvent.getEnd() != null) {
            LocalDateTime endTime = convertEventDateTime(googleEvent.getEnd(), eventTimezone);
            builder.endTime(endTime);
        }
    }
    
    private LocalDateTime convertEventDateTime(EventDateTime eventDateTime, ZoneId timezone) {
        if (eventDateTime == null) {
            return null;
        }
        
        try {
            DateTime dateTime = eventDateTime.getDateTime();
            if (dateTime != null) {
                // Event with specific time — convert to the event's own timezone
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(dateTime.getValue()),
                        timezone
                );
            } else {
                // All-day event
                DateTime date = eventDateTime.getDate();
                if (date != null) {
                    return LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(date.getValue()),
                            timezone
                    ).toLocalDate().atStartOfDay();
                }
            }
        } catch (Exception e) {
            log.warn("Error converting EventDateTime: {}", e.getMessage());
        }
        
        return null;
    }
    
    private LocalDateTime convertDateTime(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        
        try {
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(dateTime.getValue()),
                    DEFAULT_TIMEZONE
            );
        } catch (Exception e) {
            log.warn("Error converting DateTime: {}", e.getMessage());
            return null;
        }
    }
    
    private EventDateTime createEventDateTime(LocalDateTime localDateTime, ZoneId timezone) {
        if (localDateTime == null) {
            return null;
        }
        
        ZoneId effectiveTimezone = timezone != null ? timezone : DEFAULT_TIMEZONE;
        ZonedDateTime zonedDateTime = localDateTime.atZone(effectiveTimezone);
        
        return new EventDateTime()
                .setDateTime(new DateTime(zonedDateTime.toInstant().toEpochMilli()))
                .setTimeZone(effectiveTimezone.getId());
    }
    
    private ZoneId extractTimezone(EventDateTime eventDateTime) {
        if (eventDateTime != null && eventDateTime.getTimeZone() != null) {
            try {
                return ZoneId.of(eventDateTime.getTimeZone());
            } catch (Exception e) {
                log.warn("Invalid timezone: {}", eventDateTime.getTimeZone());
            }
        }
        return DEFAULT_TIMEZONE;
    }
    
    private String extractCalendarId(Event googleEvent) {
        // Google Calendar API doesn't always include calendar ID in the event
        // This would typically be set by the service layer
        return null;
    }
    
    private String extractCreator(Event googleEvent) {
        if (googleEvent.getCreator() != null) {
            return googleEvent.getCreator().getEmail();
        }
        return null;
    }
    
    private String extractMeetingUrl(Event googleEvent) {
        if (googleEvent.getHangoutLink() != null) {
            return googleEvent.getHangoutLink();
        }
        
        // Check for meeting URLs in description or location
        String description = googleEvent.getDescription();
        if (description != null && (description.contains("meet.google.com") || 
                                   description.contains("zoom.us") || 
                                   description.contains("teams.microsoft.com"))) {
            // Extract URL from description (simplified)
            String[] words = description.split("\\s+");
            for (String word : words) {
                if (word.startsWith("http") && (word.contains("meet") || word.contains("zoom") || word.contains("teams"))) {
                    return word;
                }
            }
        }
        
        return null;
    }
    
    private CalendarEvent.EventStatus mapEventStatus(String googleStatus) {
        if (googleStatus == null) {
            return CalendarEvent.EventStatus.CONFIRMED;
        }
        
        return switch (googleStatus.toLowerCase()) {
            case "confirmed" -> CalendarEvent.EventStatus.CONFIRMED;
            case "tentative" -> CalendarEvent.EventStatus.TENTATIVE;
            case "cancelled" -> CalendarEvent.EventStatus.CANCELLED;
            default -> CalendarEvent.EventStatus.CONFIRMED;
        };
    }
    
    private CalendarEvent.EventVisibility mapEventVisibility(String googleVisibility) {
        if (googleVisibility == null) {
            return CalendarEvent.EventVisibility.DEFAULT;
        }
        
        return switch (googleVisibility.toLowerCase()) {
            case "default" -> CalendarEvent.EventVisibility.DEFAULT;
            case "public" -> CalendarEvent.EventVisibility.PUBLIC;
            case "private" -> CalendarEvent.EventVisibility.PRIVATE;
            case "confidential" -> CalendarEvent.EventVisibility.CONFIDENTIAL;
            default -> CalendarEvent.EventVisibility.DEFAULT;
        };
    }
    
    private String mapToGoogleStatus(CalendarEvent.EventStatus status) {
        if (status == null) {
            return "confirmed";
        }
        
        return switch (status) {
            case CONFIRMED -> "confirmed";
            case TENTATIVE -> "tentative";
            case CANCELLED -> "cancelled";
        };
    }
    
    private String mapToGoogleVisibility(CalendarEvent.EventVisibility visibility) {
        if (visibility == null) {
            return "default";
        }
        
        return switch (visibility) {
            case DEFAULT -> "default";
            case PUBLIC -> "public";
            case PRIVATE -> "private";
            case CONFIDENTIAL -> "confidential";
        };
    }
}