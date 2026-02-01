package com.personal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.HashSet;

/**
 * Service to determine US stock market trading days and holidays.
 * NYSE/NASDAQ holidays for 2025-2027.
 */
@Service
@Slf4j
public class MarketCalendarService {

    // Static set of US market holidays
    private static final Set<LocalDate> MARKET_HOLIDAYS = new HashSet<>();

    static {
        // 2025 NYSE Holidays
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.JANUARY, 1));   // New Year's Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.JANUARY, 20));  // MLK Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.FEBRUARY, 17)); // Presidents Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.APRIL, 18));    // Good Friday
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.MAY, 26));      // Memorial Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.JUNE, 19));     // Juneteenth
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.JULY, 4));      // Independence Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.SEPTEMBER, 1)); // Labor Day
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.NOVEMBER, 27)); // Thanksgiving
        MARKET_HOLIDAYS.add(LocalDate.of(2025, Month.DECEMBER, 25)); // Christmas

        // 2026 NYSE Holidays
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.JANUARY, 1));   // New Year's Day
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.JANUARY, 19));  // MLK Day
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.FEBRUARY, 16)); // Presidents Day
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.APRIL, 3));     // Good Friday
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.MAY, 25));      // Memorial Day
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.JUNE, 19));     // Juneteenth
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.JULY, 3));      // Independence Day (observed)
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.SEPTEMBER, 7)); // Labor Day
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.NOVEMBER, 26)); // Thanksgiving
        MARKET_HOLIDAYS.add(LocalDate.of(2026, Month.DECEMBER, 25)); // Christmas

        // 2027 NYSE Holidays
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.JANUARY, 1));   // New Year's Day
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.JANUARY, 18));  // MLK Day
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.FEBRUARY, 15)); // Presidents Day
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.MARCH, 26));    // Good Friday
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.MAY, 31));      // Memorial Day
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.JUNE, 18));     // Juneteenth (observed)
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.JULY, 5));      // Independence Day (observed)
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.SEPTEMBER, 6)); // Labor Day
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.NOVEMBER, 25)); // Thanksgiving
        MARKET_HOLIDAYS.add(LocalDate.of(2027, Month.DECEMBER, 24)); // Christmas (observed)
    }

    /**
     * Check if a given date is a US market holiday.
     */
    public boolean isMarketHoliday(LocalDate date) {
        return MARKET_HOLIDAYS.contains(date);
    }

    /**
     * Check if the market is open on a given date.
     * Market is closed on weekends and holidays.
     */
    public boolean isMarketOpen(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !isMarketHoliday(date);
    }

    /**
     * Check if a given date is a trading day (market is open).
     */
    public boolean isTradingDay(LocalDate date) {
        return isMarketOpen(date);
    }

    /**
     * Get the previous trading day before the given date.
     * Skips weekends and holidays.
     */
    public LocalDate getPreviousTradingDay(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        
        // Go back until we find a trading day (limit to 10 days to avoid infinite loop)
        int attempts = 0;
        while (!isTradingDay(previous) && attempts < 10) {
            previous = previous.minusDays(1);
            attempts++;
        }
        
        return previous;
    }

    /**
     * Get the next trading day on or after the given date.
     * If the date is a trading day, returns it. Otherwise finds the next one.
     */
    public LocalDate getNextTradingDay(LocalDate date) {
        LocalDate next = date;
        
        // Go forward until we find a trading day (limit to 10 days to avoid infinite loop)
        int attempts = 0;
        while (!isTradingDay(next) && attempts < 10) {
            next = next.plusDays(1);
            attempts++;
        }
        
        return next;
    }

    /**
     * Get the most recent trading day on or before the given date.
     * If the date is a trading day, returns it. Otherwise finds the previous one.
     */
    public LocalDate getMostRecentTradingDay(LocalDate date) {
        if (isTradingDay(date)) {
            return date;
        }
        return getPreviousTradingDay(date);
    }
}
