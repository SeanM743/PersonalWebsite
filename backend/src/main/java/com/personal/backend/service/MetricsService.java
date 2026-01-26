package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Service
@Slf4j
public class MetricsService implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final MonitoringProperties monitoringProperties;
    
    // Throttling metrics
    private final Counter throttledRequestsCounter;
    private final Timer throttlingDecisionTimer;
    private final Gauge throttlingRateGauge;
    
    // Cache metrics
    private final ConcurrentHashMap<String, CacheMetrics> cacheMetricsMap = new ConcurrentHashMap<>();
    
    // Response time tracking
    private final Timer.Sample currentRequestSample;
    
    // Request tracking for throttling rate calculation
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong throttledRequests = new AtomicLong(0);
    private final DoubleAdder throttlingRate = new DoubleAdder();

    public MetricsService(MeterRegistry meterRegistry, MonitoringProperties monitoringProperties) {
        this.meterRegistry = meterRegistry;
        this.monitoringProperties = monitoringProperties;
        
        // Initialize throttling metrics
        this.throttledRequestsCounter = Counter.builder("requests_throttled_total")
                .description("Total number of throttled requests")
                .register(meterRegistry);
                
        this.throttlingDecisionTimer = Timer.builder("throttling_decision_duration")
                .description("Time taken to make throttling decisions")
                .register(meterRegistry);
                
        this.throttlingRateGauge = Gauge.builder("throttling_rate", this, MetricsService::getCurrentThrottlingRate)
                .description("Current throttling rate as percentage")
                .register(meterRegistry);
                
        this.currentRequestSample = null; // Will be set per request
        
        // Initialize cache metrics for known cache types
        initializeCacheMetrics();
        
        log.info("MetricsService initialized with monitoring enabled: {}", 
                monitoringProperties.isEnabled());
    }

    /**
     * Record a throttled request
     */
    public void recordThrottledRequest(String endpoint, String reason) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Counter.builder("throttled_requests_total")
                .description("Total throttled requests")
                .tags("endpoint", endpoint, "reason", reason)
                .register(meterRegistry)
                .increment();
        
        throttledRequests.incrementAndGet();
        updateThrottlingRate();
        
        log.debug("Recorded throttled request for endpoint: {} with reason: {}", endpoint, reason);
    }

    /**
     * Record request processing time
     */
    public Timer.Sample startRequestTimer() {
        if (!monitoringProperties.isEnabled()) {
            return null;
        }
        return Timer.start(meterRegistry);
    }

    /**
     * Stop request timer and record metrics
     */
    public void stopRequestTimer(Timer.Sample sample, String endpoint, String method, String status) {
        if (!monitoringProperties.isEnabled() || sample == null) {
            return;
        }
        
        sample.stop(Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration in seconds")
                .tags("endpoint", endpoint, "method", method, "status", status)
                .register(meterRegistry));
                
        totalRequests.incrementAndGet();
        updateThrottlingRate();
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit(String cacheType, String operation) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        CacheMetrics metrics = cacheMetricsMap.computeIfAbsent(cacheType, this::createCacheMetrics);
        metrics.hits.increment();
        
        Counter.builder("cache_operations_total")
                .description("Total cache operations")
                .tags("cache_type", cacheType, "operation", operation, "result", "hit")
                .register(meterRegistry)
                .increment();
                
        log.debug("Recorded cache hit for cache: {} operation: {}", cacheType, operation);
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String cacheType, String operation) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        CacheMetrics metrics = cacheMetricsMap.computeIfAbsent(cacheType, this::createCacheMetrics);
        metrics.misses.increment();
        
        Counter.builder("cache_operations_total")
                .description("Total cache operations")
                .tags("cache_type", cacheType, "operation", operation, "result", "miss")
                .register(meterRegistry)
                .increment();
                
        log.debug("Recorded cache miss for cache: {} operation: {}", cacheType, operation);
    }

    /**
     * Record throttling decision time
     */
    public void recordThrottlingDecision(Duration duration, String endpoint, boolean throttled) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("throttling_decision_duration")
            .description("Time taken to make throttling decisions")
            .tags("endpoint", endpoint, "throttled", String.valueOf(throttled))
            .register(meterRegistry));
    }

    /**
     * Get current throttling rate
     */
    public double getCurrentThrottlingRate() {
        long total = totalRequests.get();
        long throttled = throttledRequests.get();
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) throttled / total * 100.0;
    }

    /**
     * Get cache hit ratio for a specific cache type
     */
    public double getCacheHitRatio(String cacheType) {
        CacheMetrics metrics = cacheMetricsMap.get(cacheType);
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
     * Reset throttling metrics (useful for testing)
     */
    public void resetThrottlingMetrics() {
        totalRequests.set(0);
        throttledRequests.set(0);
        throttlingRate.reset();
    }

    /**
     * Health check implementation
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        try {
            // Check if metrics collection is working
            double currentThrottlingRate = getCurrentThrottlingRate();
            builder.withDetail("throttling_rate", currentThrottlingRate + "%");
            
            // Check cache metrics
            for (String cacheType : cacheMetricsMap.keySet()) {
                double hitRatio = getCacheHitRatio(cacheType);
                builder.withDetail("cache_hit_ratio_" + cacheType, hitRatio);
                
                // Check if cache hit ratio is below threshold
                Double threshold = monitoringProperties.getCache().getHitRatioThresholds().get(cacheType);
                if (threshold != null && hitRatio < threshold) {
                    builder.down().withDetail("cache_issue", 
                        String.format("Cache %s hit ratio %.2f below threshold %.2f", 
                            cacheType, hitRatio, threshold));
                }
            }
            
            // Check throttling rate
            if (currentThrottlingRate > monitoringProperties.getThrottling().getCriticalThreshold() * 100) {
                builder.down().withDetail("throttling_issue", 
                    "Throttling rate above critical threshold");
            } else if (currentThrottlingRate > monitoringProperties.getThrottling().getWarningThreshold() * 100) {
                builder.unknown().withDetail("throttling_warning", 
                    "Throttling rate above warning threshold");
            }
            
            builder.withDetail("total_requests", totalRequests.get());
            builder.withDetail("throttled_requests", throttledRequests.get());
            builder.withDetail("monitoring_enabled", monitoringProperties.isEnabled());
            
        } catch (Exception e) {
            log.error("Error during metrics health check", e);
            builder.down().withDetail("error", e.getMessage());
        }
        
        return builder.build();
    }

    private void updateThrottlingRate() {
        double rate = getCurrentThrottlingRate();
        throttlingRate.reset();
        throttlingRate.add(rate);
    }

    private void initializeCacheMetrics() {
        // Initialize metrics for known cache types
        String[] cacheTypes = {"api-cache", "database-cache", "portfolio-cache", "market-data-cache"};
        
        for (String cacheType : cacheTypes) {
            createCacheMetrics(cacheType);
        }
    }

    private CacheMetrics createCacheMetrics(String cacheType) {
        Counter hits = Counter.builder("cache_hits_total")
                .description("Total cache hits")
                .tags("cache_type", cacheType)
                .register(meterRegistry);
                
        Counter misses = Counter.builder("cache_misses_total")
                .description("Total cache misses")
                .tags("cache_type", cacheType)
                .register(meterRegistry);
                
        Gauge hitRatio = Gauge.builder("cache_hit_ratio", this, service -> service.getCacheHitRatio(cacheType))
                .description("Cache hit ratio")
                .tags("cache_type", cacheType)
                .register(meterRegistry);
                
        return new CacheMetrics(hits, misses, hitRatio);
    }

    private static class CacheMetrics {
        final Counter hits;
        final Counter misses;
        final Gauge hitRatio;

        CacheMetrics(Counter hits, Counter misses, Gauge hitRatio) {
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
        }
    }
}