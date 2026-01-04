package com.personal.backend.service;

import com.personal.backend.dto.EventRequest;
import com.personal.backend.util.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EventValidator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_LOCATION_LENGTH = 500;
    private static final int MIN_EVENT_DURATION_MINUTES = 1;
    private static final int MAX_EVENT_DURATION_DAYS = 365;
    private static final int MAX_ATTENDEES = 100;
    
    public ValidationResult validateEventRequest(EventRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (request == null) {
            errors.add("Event request cannot be null");
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
        }
        
        // Validate title
        validateTitle(request.getTitle(), errors);
        
        // Validate description
        validateDescription(request.getDescription(), warnings);
        
        // Validate date/time
        validateDateTime(request, errors, warnings);
        
        // Validate location
        validateLocation(request.getLocation(), warnings);
        
        // Validate attendees
        validateAttendees(request.getAttendees(), errors, warnings);
        
        // Validate meeting URL
        validateMeetingUrl(request.getMeetingUrl(), warnings);
        
        // Business logic validations
        validateBusinessRules(request, errors, warnings);
        
        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }
    
    private void validateTitle(String title, List<String> errors) {
        if (title == null || title.trim().isEmpty()) {
            errors.add("Event title is required");
            return;
        }
        
        if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("Event title must be less than " + MAX_TITLE_LENGTH + " characters");
        }
        
        // Check for potentially problematic characters
        if (title.contains("\n") || title.contains("\r")) {
            errors.add("Event title cannot contain line breaks");
        }
    }
    
    private void validateDescription(String description, List<String> warnings) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            warnings.add("Event description is very long (" + description.length() + " characters). Consider shortening it.");
        }
    }
    
    private void validateDateTime(EventRequest request, List<String> errors, List<String> warnings) {
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        
        if (startTime == null) {
            errors.add("Start time is required");
            return;
        }
        
        if (endTime == null) {
            errors.add("End time is required");
            return;
        }
        
        // Validate time range
        if (!startTime.isBefore(endTime)) {
            errors.add("Start time must be before end time");
            return;
        }
        
        // Check minimum duration
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < MIN_EVENT_DURATION_MINUTES) {
            errors.add("Event duration must be at least " + MIN_EVENT_DURATION_MINUTES + " minute(s)");
        }
        
        // Check maximum duration
        long durationDays = java.time.Duration.between(startTime, endTime).toDays();
        if (durationDays > MAX_EVENT_DURATION_DAYS) {
            errors.add("Event duration cannot exceed " + MAX_EVENT_DURATION_DAYS + " days");
        }
        
        // Warn about past events
        if (startTime.isBefore(LocalDateTime.now())) {
            warnings.add("Event is scheduled in the past");
        }
        
        // Warn about very long events
        if (durationDays > 1) {
            warnings.add("Event duration is " + durationDays + " days. Consider if this is intended.");
        }
        
        // Warn about events starting very soon
        if (startTime.isBefore(LocalDateTime.now().plusMinutes(5)) && startTime.isAfter(LocalDateTime.now())) {
            warnings.add("Event starts in less than 5 minutes");
        }
    }
    
    private void validateLocation(String location, List<String> warnings) {
        if (location != null && location.length() > MAX_LOCATION_LENGTH) {
            warnings.add("Location is very long (" + location.length() + " characters). Consider shortening it.");
        }
    }
    
    private void validateAttendees(List<String> attendees, List<String> errors, List<String> warnings) {
        if (attendees == null || attendees.isEmpty()) {
            return;
        }
        
        if (attendees.size() > MAX_ATTENDEES) {
            errors.add("Cannot have more than " + MAX_ATTENDEES + " attendees");
            return;
        }
        
        // Validate email addresses
        for (int i = 0; i < attendees.size(); i++) {
            String email = attendees.get(i);
            if (email == null || email.trim().isEmpty()) {
                errors.add("Attendee email at position " + (i + 1) + " is empty");
                continue;
            }
            
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                errors.add("Invalid email address: " + email);
            }
        }
        
        // Check for duplicates
        long uniqueCount = attendees.stream().distinct().count();
        if (uniqueCount < attendees.size()) {
            warnings.add("Duplicate attendee email addresses found");
        }
        
        // Warn about large attendee lists
        if (attendees.size() > 20) {
            warnings.add("Large number of attendees (" + attendees.size() + "). Consider if all are necessary.");
        }
    }
    
    private void validateMeetingUrl(String meetingUrl, List<String> warnings) {
        if (meetingUrl != null && !meetingUrl.trim().isEmpty()) {
            try {
                java.net.URL url = new java.net.URL(meetingUrl);
                if (!url.getProtocol().equals("https")) {
                    warnings.add("Meeting URL should use HTTPS for security");
                }
            } catch (java.net.MalformedURLException e) {
                warnings.add("Meeting URL appears to be malformed: " + meetingUrl);
            }
        }
    }
    
    private void validateBusinessRules(EventRequest request, List<String> errors, List<String> warnings) {
        // Check for reasonable working hours for business events
        if (request.getStartTime() != null) {
            int hour = request.getStartTime().getHour();
            if (hour < 6 || hour > 22) {
                warnings.add("Event is scheduled outside typical working hours (" + hour + ":00)");
            }
        }
        
        // Check for weekend events
        if (request.getStartTime() != null) {
            java.time.DayOfWeek dayOfWeek = request.getStartTime().getDayOfWeek();
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                warnings.add("Event is scheduled on a weekend (" + dayOfWeek + ")");
            }
        }
        
        // Validate all-day events
        if (request.isAllDay()) {
            if (request.getStartTime() != null && request.getEndTime() != null) {
                // For all-day events, times should be at midnight
                if (request.getStartTime().toLocalTime() != java.time.LocalTime.MIDNIGHT ||
                    request.getEndTime().toLocalTime() != java.time.LocalTime.MIDNIGHT) {
                    warnings.add("All-day events should have start and end times at midnight");
                }
            }
        }
    }
    
    public boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return false;
        }
        
        try {
            LocalDateTime.parse(timeString);
            return true;
        } catch (DateTimeParseException e) {
            log.debug("Invalid time format: {}", timeString);
            return false;
        }
    }
    
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    public boolean isReasonableDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return false;
        }
        
        long minutes = java.time.Duration.between(start, end).toMinutes();
        return minutes >= MIN_EVENT_DURATION_MINUTES && 
               minutes <= (MAX_EVENT_DURATION_DAYS * 24 * 60);
    }
}