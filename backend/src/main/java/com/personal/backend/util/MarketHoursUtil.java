package com.personal.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility for market hours detection and trading schedule management
 */
@Component
@Slf4j
public class MarketHoursUtil {
    
    @Value("${portfolio.market.hours.timezone:America/New_York}")
    private String marketTimezone;
    
    // US Market Hours (Eastern Time)
    private static final LocalTime US_MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime US_MARKET_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime US_PRE_MARKET_OPEN = LocalTime.of(4, 0);
    private static final LocalTime US_AFTER_HOURS_CLOSE = LocalTime.of(20, 0);
    
    // International Market Hours (simplified)
    private static final Map<String, MarketHours> INTERNATIONAL_MARKETS = Map.of(
            "LSE", new MarketHours("Europe/London", LocalTime.of(8, 0), LocalTime.of(16, 30)),
            "TSE", new MarketHours("Asia/Tokyo", LocalTime.of(9, 0), LocalTime.of(15, 0)),
            "SSE", new MarketHours("Asia/Shanghai", LocalTime.of(9, 30), LocalTime.of(15, 0)),
            "NSE", new MarketHours("Asia/Kolkata", LocalTime.of(9, 15), LocalTime.of(15, 30)),
            "ASX", new MarketHours("Australia/Sydney", LocalTime.of(10, 0), LocalTime.of(16, 0))
    );
    
    // US Market Holidays (simplified - would typically be loaded from external source)
    private static final Set<MonthDay> US_MARKET_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),   // New Year's Day
            MonthDay.of(7, 4),   // Independence Day
            MonthDay.of(12, 25)  // Christmas Day
            // Note: This is simplified - actual implementation would include all market holidays
            // including floating holidays like Martin Luther King Jr. Day, Presidents' Day, etc.
    );
    
    /**
     * Check if US market is currently open
     */
    public boolean isUSMarketOpen() {
        return isUSMarketOpen(ZonedDateTime.now());
    }
    
    /**
     * Check if US market is open at specific time
     */
    public boolean isUSMarketOpen(ZonedDateTime dateTime) {
        ZonedDateTime marketTime = dateTime.withZoneSameInstant(ZoneId.of(marketTimezone));
        
        // Check if it's a weekend
        DayOfWeek dayOfWeek = marketTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Check if it's a holiday
        if (isUSMarketHoliday(marketTime.toLocalDate())) {
            return false;
        }
        
        // Check if it's within trading hours
        LocalTime currentTime = marketTime.toLocalTime();
        return !currentTime.isBefore(US_MARKET_OPEN) && currentTime.isBefore(US_MARKET_CLOSE);
    }
    
    /**
     * Check if it's pre-market hours
     */
    public boolean isPreMarketHours() {
        return isPreMarketHours(ZonedDateTime.now());
    }
    
    /**
     * Check if it's pre-market hours at specific time
     */
    public boolean isPreMarketHours(ZonedDateTime dateTime) {
        ZonedDateTime marketTime = dateTime.withZoneSameInstant(ZoneId.of(marketTimezone));
        
        // Check if it's a trading day
        if (!isTradingDay(marketTime.toLocalDate())) {
            return false;
        }
        
        LocalTime currentTime = marketTime.toLocalTime();
        return !currentTime.isBefore(US_PRE_MARKET_OPEN) && currentTime.isBefore(US_MARKET_OPEN);
    }
    
    /**
     * Check if it's after-hours trading
     */
    public boolean isAfterHoursTrading() {
        return isAfterHoursTrading(ZonedDateTime.now());
    }
    
    /**
     * Check if it's after-hours trading at specific time
     */
    public boolean isAfterHoursTrading(ZonedDateTime dateTime) {
        ZonedDateTime marketTime = dateTime.withZoneSameInstant(ZoneId.of(marketTimezone));
        
        // Check if it's a trading day
        if (!isTradingDay(marketTime.toLocalDate())) {
            return false;
        }
        
        LocalTime currentTime = marketTime.toLocalTime();
        return !currentTime.isBefore(US_MARKET_CLOSE) && currentTime.isBefore(US_AFTER_HOURS_CLOSE);
    }
    
    /**
     * Check if given date is a US trading day
     */
    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && 
               dayOfWeek != DayOfWeek.SUNDAY && 
               !isUSMarketHoliday(date);
    }
    
    /**
     * Check if given date is a US market holiday
     */
    public boolean isUSMarketHoliday(LocalDate date) {
        MonthDay monthDay = MonthDay.from(date);
        
        // Check fixed holidays
        if (US_MARKET_HOLIDAYS.contains(monthDay)) {
            return true;
        }
        
        // Check if holiday falls on weekend and is observed on different day
        return isObservedHoliday(date);
    }
    
    /**
     * Get next market open time
     */
    public ZonedDateTime getNextMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        LocalDate currentDate = now.toLocalDate();
        
        // If market is currently open, return next day's open
        if (isUSMarketOpen(now)) {
            return getNextTradingDay(currentDate).atTime(US_MARKET_OPEN).atZone(ZoneId.of(marketTimezone));
        }
        
        // If it's before market open today and today is a trading day
        if (isTradingDay(currentDate) && now.toLocalTime().isBefore(US_MARKET_OPEN)) {
            return currentDate.atTime(US_MARKET_OPEN).atZone(ZoneId.of(marketTimezone));
        }
        
        // Otherwise, next trading day
        return getNextTradingDay(currentDate).atTime(US_MARKET_OPEN).atZone(ZoneId.of(marketTimezone));
    }
    
    /**
     * Get next market close time
     */
    public ZonedDateTime getNextMarketClose() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        LocalDate currentDate = now.toLocalDate();
        
        // If market is currently open
        if (isUSMarketOpen(now)) {
            return currentDate.atTime(US_MARKET_CLOSE).atZone(ZoneId.of(marketTimezone));
        }
        
        // If it's before market open today and today is a trading day
        if (isTradingDay(currentDate) && now.toLocalTime().isBefore(US_MARKET_OPEN)) {
            return currentDate.atTime(US_MARKET_CLOSE).atZone(ZoneId.of(marketTimezone));
        }
        
        // Otherwise, next trading day
        return getNextTradingDay(currentDate).atTime(US_MARKET_CLOSE).atZone(ZoneId.of(marketTimezone));
    }
    
    /**
     * Get time until market opens
     */
    public Duration getTimeUntilMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        ZonedDateTime nextOpen = getNextMarketOpen();
        return Duration.between(now, nextOpen);
    }
    
    /**
     * Get time until market closes
     */
    public Duration getTimeUntilMarketClose() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        
        if (!isUSMarketOpen(now)) {
            return Duration.ZERO;
        }
        
        ZonedDateTime nextClose = getNextMarketClose();
        return Duration.between(now, nextClose);
    }
    
    /**
     * Get current market session
     */
    public MarketSession getCurrentMarketSession() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        
        if (isUSMarketOpen(now)) {
            return MarketSession.REGULAR;
        } else if (isPreMarketHours(now)) {
            return MarketSession.PRE_MARKET;
        } else if (isAfterHoursTrading(now)) {
            return MarketSession.AFTER_HOURS;
        } else {
            return MarketSession.CLOSED;
        }
    }
    
    /**
     * Check if international market is open
     */
    public boolean isInternationalMarketOpen(String marketCode) {
        MarketHours marketHours = INTERNATIONAL_MARKETS.get(marketCode.toUpperCase());
        if (marketHours == null) {
            log.warn("Unknown market code: {}", marketCode);
            return false;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketHours.timezone()));
        
        // Check if it's a weekend (simplified - doesn't account for different weekend days)
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        
        LocalTime currentTime = now.toLocalTime();
        return !currentTime.isBefore(marketHours.openTime()) && currentTime.isBefore(marketHours.closeTime());
    }
    
    /**
     * Get market status summary
     */
    public MarketStatusSummary getMarketStatusSummary() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(marketTimezone));
        MarketSession session = getCurrentMarketSession();
        
        return new MarketStatusSummary(
                isUSMarketOpen(now),
                session,
                isTradingDay(now.toLocalDate()),
                getTimeUntilMarketOpen(),
                getTimeUntilMarketClose(),
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")),
                getNextMarketOpen(),
                getNextMarketClose()
        );
    }
    
    /**
     * Get optimal data refresh interval based on market session
     */
    public Duration getOptimalRefreshInterval() {
        MarketSession session = getCurrentMarketSession();
        
        return switch (session) {
            case REGULAR -> Duration.ofMinutes(1);      // Frequent updates during market hours
            case PRE_MARKET, AFTER_HOURS -> Duration.ofMinutes(5);  // Less frequent during extended hours
            case CLOSED -> Duration.ofMinutes(15);      // Minimal updates when closed
        };
    }
    
    /**
     * Find next trading day after given date
     */
    private LocalDate getNextTradingDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        while (!isTradingDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }
    
    /**
     * Check if holiday is observed on a different day due to weekend
     */
    private boolean isObservedHoliday(LocalDate date) {
        // New Year's Day observed
        if (isHolidayObservedOn(date, MonthDay.of(1, 1))) {
            return true;
        }
        
        // Independence Day observed
        if (isHolidayObservedOn(date, MonthDay.of(7, 4))) {
            return true;
        }
        
        // Christmas Day observed
        if (isHolidayObservedOn(date, MonthDay.of(12, 25))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a holiday is observed on given date
     */
    private boolean isHolidayObservedOn(LocalDate date, MonthDay holiday) {
        LocalDate holidayDate = holiday.atYear(date.getYear());
        DayOfWeek holidayDayOfWeek = holidayDate.getDayOfWeek();
        
        // If holiday falls on Saturday, observed on Friday
        if (holidayDayOfWeek == DayOfWeek.SATURDAY) {
            return date.equals(holidayDate.minusDays(1));
        }
        
        // If holiday falls on Sunday, observed on Monday
        if (holidayDayOfWeek == DayOfWeek.SUNDAY) {
            return date.equals(holidayDate.plusDays(1));
        }
        
        return false;
    }
    
    /**
     * Market hours record for international markets
     */
    public record MarketHours(String timezone, LocalTime openTime, LocalTime closeTime) {}
    
    /**
     * Market session enum
     */
    public enum MarketSession {
        PRE_MARKET,
        REGULAR,
        AFTER_HOURS,
        CLOSED
    }
    
    /**
     * Market status summary record
     */
    public record MarketStatusSummary(
            boolean isOpen,
            MarketSession session,
            boolean isTradingDay,
            Duration timeUntilOpen,
            Duration timeUntilClose,
            String currentTime,
            ZonedDateTime nextOpen,
            ZonedDateTime nextClose
    ) {}
}