package com.personal.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Slf4j
public class TimezoneUtil {
    
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Convert LocalDateTime from one timezone to another
     */
    public static LocalDateTime convertTimezone(LocalDateTime dateTime, ZoneId fromZone, ZoneId toZone) {
        if (dateTime == null || fromZone == null || toZone == null) {
            return dateTime;
        }
        
        try {
            ZonedDateTime zonedDateTime = dateTime.atZone(fromZone);
            return zonedDateTime.withZoneSameInstant(toZone).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Error converting timezone from {} to {}: {}", fromZone, toZone, e.getMessage());
            return dateTime;
        }
    }
    
    /**
     * Convert LocalDateTime to UTC
     */
    public static LocalDateTime toUtc(LocalDateTime dateTime, ZoneId fromZone) {
        return convertTimezone(dateTime, fromZone, ZoneOffset.UTC);
    }
    
    /**
     * Convert UTC LocalDateTime to specified timezone
     */
    public static LocalDateTime fromUtc(LocalDateTime utcDateTime, ZoneId toZone) {
        return convertTimezone(utcDateTime, ZoneOffset.UTC, toZone);
    }
    
    /**
     * Convert LocalDateTime to system default timezone
     */
    public static LocalDateTime toSystemDefault(LocalDateTime dateTime, ZoneId fromZone) {
        return convertTimezone(dateTime, fromZone, DEFAULT_TIMEZONE);
    }
    
    /**
     * Get ZoneId from string, with fallback to default
     */
    public static ZoneId parseZoneId(String timezoneString) {
        if (timezoneString == null || timezoneString.trim().isEmpty()) {
            return DEFAULT_TIMEZONE;
        }
        
        try {
            return ZoneId.of(timezoneString.trim());
        } catch (DateTimeException e) {
            log.warn("Invalid timezone '{}', using default: {}", timezoneString, DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }
    }
    
    /**
     * Check if timezone string is valid
     */
    public static boolean isValidTimezone(String timezoneString) {
        if (timezoneString == null || timezoneString.trim().isEmpty()) {
            return false;
        }
        
        try {
            ZoneId.of(timezoneString.trim());
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
    
    /**
     * Get all available timezone IDs
     */
    public static Set<String> getAvailableTimezones() {
        return ZoneId.getAvailableZoneIds();
    }
    
    /**
     * Get timezone offset for a specific date/time
     */
    public static ZoneOffset getOffset(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null || zoneId == null) {
            return ZoneOffset.UTC;
        }
        
        try {
            return zoneId.getRules().getOffset(dateTime);
        } catch (Exception e) {
            log.warn("Error getting offset for {} in {}: {}", dateTime, zoneId, e.getMessage());
            return ZoneOffset.UTC;
        }
    }
    
    /**
     * Format LocalDateTime with timezone information
     */
    public static String formatWithTimezone(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return null;
        }
        
        if (zoneId == null) {
            zoneId = DEFAULT_TIMEZONE;
        }
        
        try {
            ZonedDateTime zonedDateTime = dateTime.atZone(zoneId);
            return zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (Exception e) {
            log.warn("Error formatting datetime with timezone: {}", e.getMessage());
            return dateTime.format(ISO_FORMATTER);
        }
    }
    
    /**
     * Parse datetime string with timezone
     */
    public static LocalDateTime parseWithTimezone(String dateTimeString, ZoneId targetZone) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try parsing as zoned datetime first
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString.trim());
            if (targetZone != null) {
                return zonedDateTime.withZoneSameInstant(targetZone).toLocalDateTime();
            } else {
                return zonedDateTime.toLocalDateTime();
            }
        } catch (DateTimeParseException e) {
            // Try parsing as local datetime
            try {
                return LocalDateTime.parse(dateTimeString.trim(), ISO_FORMATTER);
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse datetime string: {}", dateTimeString);
                return null;
            }
        }
    }
    
    /**
     * Check if two LocalDateTime instances overlap when considering timezones
     */
    public static boolean doTimesOverlap(
            LocalDateTime start1, LocalDateTime end1, ZoneId zone1,
            LocalDateTime start2, LocalDateTime end2, ZoneId zone2) {
        
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        
        // Convert all times to UTC for comparison
        LocalDateTime utcStart1 = toUtc(start1, zone1 != null ? zone1 : DEFAULT_TIMEZONE);
        LocalDateTime utcEnd1 = toUtc(end1, zone1 != null ? zone1 : DEFAULT_TIMEZONE);
        LocalDateTime utcStart2 = toUtc(start2, zone2 != null ? zone2 : DEFAULT_TIMEZONE);
        LocalDateTime utcEnd2 = toUtc(end2, zone2 != null ? zone2 : DEFAULT_TIMEZONE);
        
        // Check for overlap: start1 < end2 && start2 < end1
        return utcStart1.isBefore(utcEnd2) && utcStart2.isBefore(utcEnd1);
    }
    
    /**
     * Get user-friendly timezone display name
     */
    public static String getTimezoneDisplayName(ZoneId zoneId) {
        if (zoneId == null) {
            return DEFAULT_TIMEZONE.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault());
        }
        
        return zoneId.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault());
    }
    
    /**
     * Check if datetime is in business hours for given timezone
     */
    public static boolean isBusinessHours(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return false;
        }
        
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        
        // Consider business hours as 9 AM to 5 PM, Monday to Friday
        boolean isWeekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        boolean isBusinessTime = time.isAfter(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(17, 0));
        
        return isWeekday && isBusinessTime;
    }
    
    /**
     * Get the default timezone
     */
    public static ZoneId getDefaultTimezone() {
        return DEFAULT_TIMEZONE;
    }
}