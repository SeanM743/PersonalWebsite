package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMetricsService {

    private final MeterRegistry meterRegistry;
    private final MonitoringProperties monitoringProperties;
    
    // Performance tracking
    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    /**
     * Record HTTP request metrics
     */
    public void recordHttpRequest(String endpoint, String method, int statusCode, Duration duration) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        // Record basic HTTP metrics
        Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration in seconds")
                .tags("endpoint", endpoint, "method", method, "status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(duration);
        
        // Record endpoint-specific metrics
        EndpointMetrics metrics = getOrCreateEndpointMetrics(endpoint);
        metrics.requestCount.increment();
        
        if (statusCode >= 400) {
            metrics.errorCount.increment();
            recordError(endpoint, statusCode);
        }
        
        // Record response time percentiles
        DistributionSummary.builder("http_request_size_bytes")
                .description("HTTP request size in bytes")
                .tags("endpoint", endpoint, "method", method)
                .register(meterRegistry)
                .record(duration.toMillis()); // Using duration as proxy for size
    }

    /**
     * Record database operation metrics
     */
    @Timed(value = "database_operation_duration", description = "Database operation duration")
    public void recordDatabaseOperation(String operation, String table, Duration duration, boolean success) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Timer.builder("database_operation_duration_seconds")
                .description("Database operation duration in seconds")
                .tags("operation", operation, "table", table, "success", String.valueOf(success))
                .register(meterRegistry)
                .record(duration);
        
        Counter.builder("database_operations_total")
                .description("Total database operations")
                .tags("operation", operation, "table", table, "success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record external API call metrics
     */
    public void recordExternalApiCall(String service, String endpoint, Duration duration, boolean success, int statusCode) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Timer.builder("external_api_duration_seconds")
                .description("External API call duration in seconds")
                .tags("service", service, "endpoint", endpoint, "success", String.valueOf(success), "status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(duration);
        
        Counter.builder("external_api_calls_total")
                .description("Total external API calls")
                .tags("service", service, "endpoint", endpoint, "success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record business operation metrics
     */
    public void recordBusinessOperation(String operation, Duration duration, boolean success) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Timer.builder("business_operation_duration_seconds")
                .description("Business operation duration in seconds")
                .tags("operation", operation, "success", String.valueOf(success))
                .register(meterRegistry)
                .record(duration);
        
        Counter.builder("business_operations_total")
                .description("Total business operations")
                .tags("operation", operation, "success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record memory usage metrics
     */
    public void recordMemoryUsage() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Gauge.builder("jvm_memory_used_bytes", runtime, r -> (double) (r.totalMemory() - r.freeMemory()))
                .description("Used JVM memory in bytes")
                .register(meterRegistry);
        
        Gauge.builder("jvm_memory_total_bytes", runtime, r -> (double) r.totalMemory())
                .description("Total JVM memory in bytes")
                .register(meterRegistry);
        
        Gauge.builder("jvm_memory_max_bytes", runtime, r -> (double) r.maxMemory())
                .description("Max JVM memory in bytes")
                .register(meterRegistry);
        
        Gauge.builder("jvm_memory_utilization_percent", runtime, r -> {
            long used = r.totalMemory() - r.freeMemory();
            return (double) used / r.maxMemory() * 100;
        })
                .description("JVM memory utilization percentage")
                .register(meterRegistry);
    }

    /**
     * Get error rate for an endpoint
     */
    public double getErrorRate(String endpoint) {
        EndpointMetrics metrics = endpointMetrics.get(endpoint);
        if (metrics == null) {
            return 0.0;
        }
        
        double totalRequests = metrics.requestCount.count();
        double errorRequests = metrics.errorCount.count();
        
        if (totalRequests == 0) {
            return 0.0;
        }
        
        return errorRequests / totalRequests;
    }

    /**
     * Get average response time for an endpoint
     */
    public double getAverageResponseTime(String endpoint) {
        // This would typically be retrieved from the Timer metrics
        // For now, return a placeholder
        return 0.0;
    }

    /**
     * Record custom metric
     */
    public void recordCustomMetric(String name, String description, double value, String... tags) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Tags metricTags = Tags.empty();
        if (tags != null && tags.length > 0 && tags.length % 2 == 0) {
            for (int i = 0; i < tags.length; i += 2) {
                metricTags = metricTags.and(tags[i], tags[i + 1]);
            }
        }
        
        Gauge.builder(name, value, val -> (double) val)
                .description(description)
                .tags(metricTags)
                .register(meterRegistry);
    }

    /**
     * Increment custom counter
     */
    public void incrementCustomCounter(String name, String description, String... tags) {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        Tags metricTags = Tags.empty();
        if (tags != null && tags.length > 0 && tags.length % 2 == 0) {
            for (int i = 0; i < tags.length; i += 2) {
                metricTags = metricTags.and(tags[i], tags[i + 1]);
            }
        }
        
        Counter.builder(name)
                .description(description)
                .tags(metricTags)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Start performance timer
     */
    public Timer.Sample startTimer() {
        if (!monitoringProperties.isEnabled()) {
            return null;
        }
        return Timer.start(meterRegistry);
    }

    /**
     * Stop performance timer with custom name
     */
    public void stopTimer(Timer.Sample sample, String timerName, String description, String... tags) {
        if (!monitoringProperties.isEnabled() || sample == null) {
            return;
        }
        
        Tags metricTags = Tags.empty();
        if (tags != null && tags.length > 0 && tags.length % 2 == 0) {
            for (int i = 0; i < tags.length; i += 2) {
                metricTags = metricTags.and(tags[i], tags[i + 1]);
            }
        }
        
        sample.stop(Timer.builder(timerName)
                .description(description)
                .tags(metricTags)
                .register(meterRegistry));
    }

    private void recordError(String endpoint, int statusCode) {
        String errorKey = endpoint + ":" + statusCode;
        errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        
        Counter.builder("http_errors_total")
                .description("Total HTTP errors")
                .tags("endpoint", endpoint, "status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    private EndpointMetrics getOrCreateEndpointMetrics(String endpoint) {
        return endpointMetrics.computeIfAbsent(endpoint, ep -> {
            Counter requestCount = Counter.builder("endpoint_requests_total")
                    .description("Total requests per endpoint")
                    .tags("endpoint", ep)
                    .register(meterRegistry);
                    
            Counter errorCount = Counter.builder("endpoint_errors_total")
                    .description("Total errors per endpoint")
                    .tags("endpoint", ep)
                    .register(meterRegistry);
                    
            Gauge errorRate = Gauge.builder("endpoint_error_rate", this, service -> service.getErrorRate(ep))
                    .description("Error rate per endpoint")
                    .tags("endpoint", ep)
                    .register(meterRegistry);
                    
            return new EndpointMetrics(requestCount, errorCount, errorRate);
        });
    }

    private static class EndpointMetrics {
        final Counter requestCount;
        final Counter errorCount;
        final Gauge errorRate;

        EndpointMetrics(Counter requestCount, Counter errorCount, Gauge errorRate) {
            this.requestCount = requestCount;
            this.errorCount = errorCount;
            this.errorRate = errorRate;
        }
    }
}