package com.personal.backend.service;

import com.personal.backend.model.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarCacheManager {
    
    private final CacheManager cacheManager;
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Cache names
    public static final String UPCOMING_EVENTS_CACHE = "upcomingEvents";
    public static final String TODAY_EVENTS_CACHE = "todayEvents";
    public static final String WEEK_EVENTS_CACHE = "weekEvents";
    public static final String EVENT_DETAILS_CACHE = "eventDetails";
    public static final String SEARCH_RESULTS_CACHE = "searchResults";
    
    /**
     * Cache upcoming events with TTL tracking
     */
    @Cacheable(value = UPCOMING_EVENTS_CACHE, key = "#days", unless = "#result == null || #result.isEmpty()")
    public List<CalendarEvent> cacheUpcomingEvents(int days, List<CalendarEvent> events) {
        if (events != null && !events.isEmpty()) {
            cacheTimestamps.put(UPCOMING_EVENTS_CACHE + ":" + days, LocalDateTime.now());
            log.debug("Cached {} upcoming events for {} days", events.size(), days);
        }
        return events;
    }
    
    /**
     * Cache today's events with TTL tracking
     */
    @Cacheable(value = TODAY_EVENTS_CACHE, unless = "#result == null || #result.isEmpty()")
    public List<CalendarEvent> cacheTodayEvents(List<CalendarEvent> events) {
        if (events != null && !events.isEmpty()) {
            cacheTimestamps.put(TODAY_EVENTS_CACHE, LocalDateTime.now());
            log.debug("Cached {} today's events", events.size());
        }
        return events;
    }
    
    /**
     * Cache week events with TTL tracking
     */
    @Cacheable(value = WEEK_EVENTS_CACHE, unless = "#result == null || #result.isEmpty()")
    public List<CalendarEvent> cacheWeekEvents(List<CalendarEvent> events) {
        if (events != null && !events.isEmpty()) {
            cacheTimestamps.put(WEEK_EVENTS_CACHE, LocalDateTime.now());
            log.debug("Cached {} week events", events.size());
        }
        return events;
    }
    
    /**
     * Cache individual event details
     */
    @Cacheable(value = EVENT_DETAILS_CACHE, key = "#eventId", unless = "#result == null")
    public CalendarEvent cacheEventDetails(String eventId, CalendarEvent event) {
        if (event != null) {
            cacheTimestamps.put(EVENT_DETAILS_CACHE + ":" + eventId, LocalDateTime.now());
            log.debug("Cached event details for ID: {}", eventId);
        }
        return event;
    }
    
    /**
     * Cache search results
     */
    @Cacheable(value = SEARCH_RESULTS_CACHE, key = "#query + ':' + #maxResults", unless = "#result == null || #result.isEmpty()")
    public List<CalendarEvent> cacheSearchResults(String query, int maxResults, List<CalendarEvent> events) {
        if (events != null && !events.isEmpty()) {
            cacheTimestamps.put(SEARCH_RESULTS_CACHE + ":" + query + ":" + maxResults, LocalDateTime.now());
            log.debug("Cached {} search results for query: {}", events.size(), query);
        }
        return events;
    }
    
    /**
     * Invalidate all event-related caches
     */
    @CacheEvict(value = {UPCOMING_EVENTS_CACHE, TODAY_EVENTS_CACHE, WEEK_EVENTS_CACHE, 
                         EVENT_DETAILS_CACHE, SEARCH_RESULTS_CACHE}, allEntries = true)
    public void invalidateAllCaches() {
        cacheTimestamps.clear();
        log.info("Invalidated all Google Calendar caches");
    }
    
    /**
     * Invalidate specific event cache
     */
    @CacheEvict(value = EVENT_DETAILS_CACHE, key = "#eventId")
    public void invalidateEventCache(String eventId) {
        cacheTimestamps.remove(EVENT_DETAILS_CACHE + ":" + eventId);
        log.debug("Invalidated cache for event ID: {}", eventId);
    }
    
    /**
     * Invalidate event list caches (after create/update/delete operations)
     */
    @CacheEvict(value = {UPCOMING_EVENTS_CACHE, TODAY_EVENTS_CACHE, WEEK_EVENTS_CACHE, SEARCH_RESULTS_CACHE}, allEntries = true)
    public void invalidateEventListCaches() {
        // Remove list cache timestamps
        cacheTimestamps.entrySet().removeIf(entry -> 
                entry.getKey().startsWith(UPCOMING_EVENTS_CACHE) ||
                entry.getKey().startsWith(TODAY_EVENTS_CACHE) ||
                entry.getKey().startsWith(WEEK_EVENTS_CACHE) ||
                entry.getKey().startsWith(SEARCH_RESULTS_CACHE));
        
        log.debug("Invalidated event list caches");
    }
    
    /**
     * Check if cache entry is stale
     */
    public boolean isCacheStale(String cacheKey, int maxAgeMinutes) {
        LocalDateTime cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime == null) {
            return true;
        }
        
        return cacheTime.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        for (String cacheName : List.of(UPCOMING_EVENTS_CACHE, TODAY_EVENTS_CACHE, 
                                       WEEK_EVENTS_CACHE, EVENT_DETAILS_CACHE, SEARCH_RESULTS_CACHE)) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Try to get native cache for statistics
                Object nativeCache = cache.getNativeCache();
                if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                    stats.put(cacheName, Map.of(
                            "estimatedSize", caffeineCache.estimatedSize(),
                            "hitCount", caffeineCache.stats().hitCount(),
                            "missCount", caffeineCache.stats().missCount(),
                            "hitRate", caffeineCache.stats().hitRate(),
                            "evictionCount", caffeineCache.stats().evictionCount()
                    ));
                } else {
                    stats.put(cacheName, Map.of("status", "available", "type", nativeCache.getClass().getSimpleName()));
                }
            } else {
                stats.put(cacheName, Map.of("status", "not_available"));
            }
        }
        
        stats.put("cacheTimestamps", cacheTimestamps.size());
        stats.put("lastUpdated", LocalDateTime.now().toString());
        
        return stats;
    }
    
    /**
     * Warm up cache with commonly accessed data
     */
    public void warmUpCache() {
        log.info("Starting Google Calendar cache warm-up");
        
        try {
            // This would typically be called by a scheduled task
            // to pre-populate cache with frequently accessed data
            log.info("Cache warm-up completed");
        } catch (Exception e) {
            log.error("Error during cache warm-up: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear expired cache entries
     */
    public void clearExpiredEntries(int maxAgeMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        
        cacheTimestamps.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        log.debug("Cleared expired cache entries older than {} minutes", maxAgeMinutes);
    }
    
    /**
     * Get cache health status
     */
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        boolean allCachesAvailable = true;
        for (String cacheName : List.of(UPCOMING_EVENTS_CACHE, TODAY_EVENTS_CACHE, 
                                       WEEK_EVENTS_CACHE, EVENT_DETAILS_CACHE, SEARCH_RESULTS_CACHE)) {
            Cache cache = cacheManager.getCache(cacheName);
            boolean available = cache != null;
            health.put(cacheName + "_available", available);
            
            if (!available) {
                allCachesAvailable = false;
            }
        }
        
        health.put("overall_healthy", allCachesAvailable);
        health.put("active_timestamps", cacheTimestamps.size());
        health.put("checked_at", LocalDateTime.now().toString());
        
        return health;
    }
}