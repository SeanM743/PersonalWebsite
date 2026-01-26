package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsService {

    private final MeterRegistry meterRegistry;
    private final MonitoringProperties monitoringProperties;
    private final CacheManager cacheManager;
    
    // Cache operation tracking
    private final ConcurrentHashMap<String, CacheOperationMetrics> cacheMetrics = new ConcurrentHashMap<>();
    
    // Time window tracking for hit ratios
    private final ConcurrentHashMap<String, TimeWindowMetrics> timeWindowMetrics = new ConcurrentHashMap<>();

    /**
     * Record cache hit operation
     */
    public void recordCacheHit(String cacheName, String operation, String key) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        CacheOperationMetrics metrics = getOrCreateCacheMetrics(cacheName);
        metrics.hits.increment();
        
        // Record in time window
        TimeWindowMetrics windowMetrics = getOrCreateTimeWindowMetrics(cacheName);
        windowMetrics.recordHit();
        
        // Record detailed cache operation
        Counter.builder("cache_operations_total")
                .description("Total cache operations by type and result")
                .tags("cache_name", cacheName, "operation", operation, "result", "hit")
                .register(meterRegistry)
                .increment();
                
        log.debug("Cache hit recorded: cache={}, operation={}, key={}", cacheName, operation, key);
    }

    /**
     * Record cache miss operation
     */
    public void recordCacheMiss(String cacheName, String operation, String key) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        CacheOperationMetrics metrics = getOrCreateCacheMetrics(cacheName);
        metrics.misses.increment();
        
        // Record in time window
        TimeWindowMetrics windowMetrics = getOrCreateTimeWindowMetrics(cacheName);
        windowMetrics.recordMiss();
        
        // Record detailed cache operation
        Counter.builder("cache_operations_total")
                .description("Total cache operations by type and result")
                .tags("cache_name", cacheName, "operation", operation, "result", "miss")
                .register(meterRegistry)
                .increment();
                
        log.debug("Cache miss recorded: cache={}, operation={}, key={}", cacheName, operation, key);
    }

    /**
     * Record cache eviction
     */
    public void recordCacheEviction(String cacheName, String reason, String key) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Counter.builder("cache_evictions_total")
                .description("Total cache evictions by reason")
                .tags("cache_name", cacheName, "reason", reason)
                .register(meterRegistry)
                .increment();
                
        log.debug("Cache eviction recorded: cache={}, reason={}, key={}", cacheName, reason, key);
    }

    /**
     * Record cache operation timing
     */
    public Timer.Sample startCacheOperationTimer(String cacheName, String operation) {
        if (!monitoringProperties.isEnabled()) {
            return null;
        }
        return Timer.start(meterRegistry);
    }

    /**
     * Stop cache operation timer
     */
    public void stopCacheOperationTimer(Timer.Sample sample, String cacheName, String operation, boolean hit) {
        if (!monitoringProperties.isEnabled() || sample == null) {
            return;
        }
        
        sample.stop(Timer.builder("cache_operation_duration_seconds")
                .description("Cache operation duration in seconds")
                .tags("cache_name", cacheName, "operation", operation, "result", hit ? "hit" : "miss")
                .register(meterRegistry));
    }

    /**
     * Get current cache hit ratio for a specific cache
     */
    public double getCacheHitRatio(String cacheName) {
        CacheOperationMetrics metrics = cacheMetrics.get(cacheName);
        if (metrics == null) {
            return 0.0;
        }
        
        double hits = metrics.hits.count();
        double misses = metrics.misses.count();
        double total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return hits / total;
    }

    /**
     * Get cache hit ratio for a specific time window (last N minutes)
     */
    public double getCacheHitRatioForWindow(String cacheName, int minutes) {
        TimeWindowMetrics windowMetrics = timeWindowMetrics.get(cacheName);
        if (windowMetrics == null) {
            return 0.0;
        }
        
        return windowMetrics.getHitRatioForWindow(minutes);
    }

    /**
     * Get cache size if available
     */
    public long getCacheSize(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache = 
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
                return caffeineCache.estimatedSize();
            }
        } catch (Exception e) {
            log.warn("Could not get cache size for cache: {}", cacheName, e);
        }
        return -1;
    }

    /**
     * Get all cache names being monitored
     */
    public java.util.Set<String> getMonitoredCacheNames() {
        return cacheMetrics.keySet();
    }

    /**
     * Reset cache metrics for testing
     */
    public void resetCacheMetrics(String cacheName) {
        cacheMetrics.remove(cacheName);
        timeWindowMetrics.remove(cacheName);
    }

    /**
     * Get cache statistics summary
     */
    public CacheStatsSummary getCacheStatsSummary(String cacheName) {
        CacheOperationMetrics metrics = cacheMetrics.get(cacheName);
        if (metrics == null) {
            return new CacheStatsSummary(cacheName, 0, 0, 0.0, -1);
        }
        
        long hits = (long) metrics.hits.count();
        long misses = (long) metrics.misses.count();
        double hitRatio = getCacheHitRatio(cacheName);
        long size = getCacheSize(cacheName);
        
        return new CacheStatsSummary(cacheName, hits, misses, hitRatio, size);
    }

    private CacheOperationMetrics getOrCreateCacheMetrics(String cacheName) {
        return cacheMetrics.computeIfAbsent(cacheName, name -> {
            Counter hits = Counter.builder("cache_hits_total")
                    .description("Total cache hits")
                    .tags("cache_name", name)
                    .register(meterRegistry);
                    
            Counter misses = Counter.builder("cache_misses_total")
                    .description("Total cache misses")
                    .tags("cache_name", name)
                    .register(meterRegistry);
                    
            Gauge hitRatio = Gauge.builder("cache_hit_ratio", this, service -> service.getCacheHitRatio(name))
                    .description("Cache hit ratio")
                    .tags("cache_name", name)
                    .register(meterRegistry);
                    
            Gauge cacheSize = Gauge.builder("cache_size", this, service -> service.getCacheSize(name))
                    .description("Current cache size")
                    .tags("cache_name", name)
                    .register(meterRegistry);
                    
            return new CacheOperationMetrics(hits, misses, hitRatio, cacheSize);
        });
    }

    private TimeWindowMetrics getOrCreateTimeWindowMetrics(String cacheName) {
        return timeWindowMetrics.computeIfAbsent(cacheName, TimeWindowMetrics::new);
    }

    private static class CacheOperationMetrics {
        final Counter hits;
        final Counter misses;
        final Gauge hitRatio;
        final Gauge cacheSize;

        CacheOperationMetrics(Counter hits, Counter misses, Gauge hitRatio, Gauge cacheSize) {
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
            this.cacheSize = cacheSize;
        }
    }

    private static class TimeWindowMetrics {
        private final String cacheName;
        private final ConcurrentHashMap<Long, WindowData> windows = new ConcurrentHashMap<>();
        private static final int WINDOW_SIZE_MINUTES = 1; // 1-minute windows

        TimeWindowMetrics(String cacheName) {
            this.cacheName = cacheName;
        }

        void recordHit() {
            long windowKey = getCurrentWindowKey();
            windows.computeIfAbsent(windowKey, k -> new WindowData()).hits.incrementAndGet();
            cleanOldWindows();
        }

        void recordMiss() {
            long windowKey = getCurrentWindowKey();
            windows.computeIfAbsent(windowKey, k -> new WindowData()).misses.incrementAndGet();
            cleanOldWindows();
        }

        double getHitRatioForWindow(int minutes) {
            long currentWindow = getCurrentWindowKey();
            long startWindow = currentWindow - minutes;
            
            long totalHits = 0;
            long totalMisses = 0;
            
            for (long window = startWindow; window <= currentWindow; window++) {
                WindowData data = windows.get(window);
                if (data != null) {
                    totalHits += data.hits.get();
                    totalMisses += data.misses.get();
                }
            }
            
            long total = totalHits + totalMisses;
            return total == 0 ? 0.0 : (double) totalHits / total;
        }

        private long getCurrentWindowKey() {
            return Instant.now().toEpochMilli() / (WINDOW_SIZE_MINUTES * 60 * 1000);
        }

        private void cleanOldWindows() {
            long currentWindow = getCurrentWindowKey();
            long cutoff = currentWindow - 60; // Keep last 60 minutes of data
            
            windows.entrySet().removeIf(entry -> entry.getKey() < cutoff);
        }

        private static class WindowData {
            final AtomicLong hits = new AtomicLong(0);
            final AtomicLong misses = new AtomicLong(0);
        }
    }

    public static class CacheStatsSummary {
        public final String cacheName;
        public final long hits;
        public final long misses;
        public final double hitRatio;
        public final long size;

        public CacheStatsSummary(String cacheName, long hits, long misses, double hitRatio, long size) {
            this.cacheName = cacheName;
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
            this.size = size;
        }
    }
}