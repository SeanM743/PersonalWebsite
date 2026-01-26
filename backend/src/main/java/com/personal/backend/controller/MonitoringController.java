package com.personal.backend.controller;

import com.personal.backend.security.MonitoringAccessControl;
import com.personal.backend.service.CacheMetricsService;
import com.personal.backend.service.MetricsService;
import com.personal.backend.service.PerformanceMetricsService;
import com.personal.backend.service.ServiceHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class MonitoringController {

    private final ServiceHealthService serviceHealthService;
    private final MetricsService metricsService;
    private final CacheMetricsService cacheMetricsService;
    private final PerformanceMetricsService performanceMetricsService;
    private final MonitoringAccessControl accessControl;

    /**
     * Get overall service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Health health = serviceHealthService.health();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", health.getStatus().getCode());
            response.put("details", health.getDetails());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving health status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve health status", "message", e.getMessage()));
        }
    }

    /**
     * Get detailed health status for all services
     */
    @GetMapping("/health/services")
    public ResponseEntity<Map<String, Object>> getServicesHealth() {
        try {
            Collection<ServiceHealthService.ServiceHealthStatus> services = 
                    serviceHealthService.getAllServiceHealth();
            
            List<Map<String, Object>> serviceList = services.stream()
                    .map(service -> {
                        Map<String, Object> serviceMap = new HashMap<>();
                        serviceMap.put("serviceName", service.serviceName);
                        serviceMap.put("status", service.status.name());
                        serviceMap.put("message", service.message);
                        serviceMap.put("responseTime", service.responseTime.toMillis());
                        serviceMap.put("lastCheckTime", service.lastCheckTime.toString());
                        
                        // Calculate health score based on status
                        int healthScore;
                        switch (service.status) {
                            case UP:
                                healthScore = 100;
                                break;
                            case DEGRADED:
                                healthScore = 60;
                                break;
                            case DOWN:
                                healthScore = 0;
                                break;
                            case UNKNOWN:
                            default:
                                healthScore = 50;
                                break;
                        }
                        serviceMap.put("healthScore", healthScore);
                        
                        return serviceMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", serviceList);
            response.put("overallScore", serviceHealthService.getOverallHealthScore());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving services health", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve services health");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get specific service health status
     */
    @GetMapping("/health/services/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceHealth(@PathVariable String serviceName) {
        try {
            ServiceHealthService.ServiceHealthStatus service = 
                    serviceHealthService.getServiceHealth(serviceName);
            
            if (service == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("name", service.serviceName);
            response.put("status", service.status.name());
            response.put("message", service.message);
            response.put("responseTime", service.responseTime.toMillis());
            response.put("lastCheck", service.lastCheckTime.toEpochMilli());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving service health for: {}", serviceName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve service health", "message", e.getMessage()));
        }
    }

    /**
     * Get throttling metrics
     */
    @GetMapping("/metrics/throttling")
    public ResponseEntity<Map<String, Object>> getThrottlingMetrics() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("currentRate", metricsService.getCurrentThrottlingRate());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving throttling metrics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve throttling metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get cache metrics summary
     */
    @GetMapping("/metrics/cache")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        try {
            Set<String> cacheNames = cacheMetricsService.getMonitoredCacheNames();
            
            List<Map<String, Object>> cacheList = cacheNames.stream()
                    .map(cacheName -> {
                        CacheMetricsService.CacheStatsSummary stats = 
                                cacheMetricsService.getCacheStatsSummary(cacheName);
                        
                        Map<String, Object> cacheMap = new HashMap<>();
                        cacheMap.put("cacheName", stats.cacheName);
                        cacheMap.put("hits", stats.hits);
                        cacheMap.put("misses", stats.misses);
                        cacheMap.put("hitRatio", stats.hitRatio);
                        cacheMap.put("size", stats.size);
                        cacheMap.put("evictions", 0); // TODO: add evictions tracking
                        cacheMap.put("maxSize", 1000); // TODO: get from cache config
                        cacheMap.put("avgLoadTime", 0); // TODO: add load time tracking
                        return cacheMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", cacheList);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving cache metrics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve cache metrics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get cache metrics for specific cache
     */
    @GetMapping("/metrics/cache/{cacheName}")
    public ResponseEntity<Map<String, Object>> getCacheMetrics(@PathVariable String cacheName) {
        try {
            CacheMetricsService.CacheStatsSummary stats = 
                    cacheMetricsService.getCacheStatsSummary(cacheName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("name", stats.cacheName);
            response.put("hits", stats.hits);
            response.put("misses", stats.misses);
            response.put("hitRatio", stats.hitRatio);
            response.put("size", stats.size);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving cache metrics for: {}", cacheName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve cache metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get cache hit ratio for specific time window
     */
    @GetMapping("/metrics/cache/{cacheName}/window/{minutes}")
    public ResponseEntity<Map<String, Object>> getCacheHitRatioWindow(
            @PathVariable String cacheName,
            @PathVariable int minutes) {
        try {
            double hitRatio = cacheMetricsService.getCacheHitRatioForWindow(cacheName, minutes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("cacheName", cacheName);
            response.put("windowMinutes", minutes);
            response.put("hitRatio", hitRatio);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving cache hit ratio window for: {}", cacheName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve cache hit ratio", "message", e.getMessage()));
        }
    }

    /**
     * Get overall metrics summary
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Health metrics
            response.put("healthScore", serviceHealthService.getOverallHealthScore());
            
            // Throttling metrics
            response.put("throttlingRate", metricsService.getCurrentThrottlingRate());
            
            // Cache metrics
            Set<String> cacheNames = cacheMetricsService.getMonitoredCacheNames();
            double avgHitRatio = cacheNames.stream()
                    .mapToDouble(cacheMetricsService::getCacheHitRatio)
                    .average()
                    .orElse(0.0);
            response.put("avgCacheHitRatio", avgHitRatio);
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving metrics summary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve metrics summary", "message", e.getMessage()));
        }
    }

    /**
     * Trigger manual health check
     */
    @PostMapping("/health/check")
    public ResponseEntity<Map<String, Object>> triggerHealthCheck() {
        if (!accessControl.canTriggerHealthChecks()) {
            accessControl.logAccessAttempt("trigger-health-check", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Insufficient permissions to trigger health checks"));
        }
        
        try {
            accessControl.logAccessAttempt("trigger-health-check", true);
            serviceHealthService.performScheduledHealthChecks();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Health check triggered successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering health check", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to trigger health check", "message", e.getMessage()));
        }
    }

    /**
     * Reset throttling metrics (for testing)
     */
    @PostMapping("/metrics/throttling/reset")
    public ResponseEntity<Map<String, Object>> resetThrottlingMetrics() {
        if (!accessControl.canResetMetrics()) {
            accessControl.logAccessAttempt("reset-throttling-metrics", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Insufficient permissions to reset metrics"));
        }
        
        try {
            accessControl.logAccessAttempt("reset-throttling-metrics", true);
            metricsService.resetThrottlingMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Throttling metrics reset successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting throttling metrics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reset throttling metrics", "message", e.getMessage()));
        }
    }

    /**
     * Reset cache metrics (for testing)
     */
    @PostMapping("/metrics/cache/{cacheName}/reset")
    public ResponseEntity<Map<String, Object>> resetCacheMetrics(@PathVariable String cacheName) {
        if (!accessControl.canResetMetrics()) {
            accessControl.logAccessAttempt("reset-cache-metrics", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Insufficient permissions to reset metrics"));
        }
        
        try {
            accessControl.logAccessAttempt("reset-cache-metrics", true);
            cacheMetricsService.resetCacheMetrics(cacheName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cache metrics reset successfully");
            response.put("cacheName", cacheName);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting cache metrics for: {}", cacheName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reset cache metrics", "message", e.getMessage()));
        }
    }

    /**
     * Webhook endpoint for external service updates
     */
    @PostMapping("/webhook/health")
    public ResponseEntity<Map<String, Object>> healthWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received health webhook: {}", payload);
            
            // Process webhook payload
            String serviceName = (String) payload.get("service");
            String status = (String) payload.get("status");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Webhook received successfully");
            response.put("service", serviceName);
            response.put("status", status);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing health webhook", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process webhook", "message", e.getMessage()));
        }
    }
}
