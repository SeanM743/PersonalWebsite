package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ServiceHealthService serviceHealthService;
    private final MetricsService metricsService;
    private final CacheMetricsService cacheMetricsService;
    private final MonitoringProperties monitoringProperties;

    /**
     * Broadcast health metrics every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void broadcastHealthMetrics() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }

        try {
            Map<String, Object> healthData = new HashMap<>();
            
            // Overall health score
            healthData.put("healthScore", serviceHealthService.getOverallHealthScore());
            
            // Individual service health
            Collection<ServiceHealthService.ServiceHealthStatus> services = 
                    serviceHealthService.getAllServiceHealth();
            
            List<Map<String, Object>> serviceList = services.stream()
                    .map(service -> {
                        Map<String, Object> serviceMap = new HashMap<>();
                        serviceMap.put("name", service.serviceName);
                        serviceMap.put("status", service.status.name());
                        serviceMap.put("message", service.message);
                        serviceMap.put("responseTime", service.responseTime.toMillis());
                        return serviceMap;
                    })
                    .collect(Collectors.toList());
            
            healthData.put("services", serviceList);
            healthData.put("timestamp", System.currentTimeMillis());
            
            // Broadcast to all subscribers
            messagingTemplate.convertAndSend("/topic/health", healthData);
            
            log.debug("Broadcasted health metrics to subscribers");
        } catch (Exception e) {
            log.error("Error broadcasting health metrics", e);
        }
    }

    /**
     * Broadcast throttling metrics every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastThrottlingMetrics() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }

        try {
            Map<String, Object> throttlingData = new HashMap<>();
            throttlingData.put("rate", metricsService.getCurrentThrottlingRate());
            throttlingData.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/throttling", throttlingData);
            
            log.debug("Broadcasted throttling metrics to subscribers");
        } catch (Exception e) {
            log.error("Error broadcasting throttling metrics", e);
        }
    }

    /**
     * Broadcast cache metrics every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void broadcastCacheMetrics() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }

        try {
            Set<String> cacheNames = cacheMetricsService.getMonitoredCacheNames();
            
            List<Map<String, Object>> cacheList = cacheNames.stream()
                    .map(cacheName -> {
                        CacheMetricsService.CacheStatsSummary stats = 
                                cacheMetricsService.getCacheStatsSummary(cacheName);
                        
                        Map<String, Object> cacheMap = new HashMap<>();
                        cacheMap.put("name", stats.cacheName);
                        cacheMap.put("hits", stats.hits);
                        cacheMap.put("misses", stats.misses);
                        cacheMap.put("hitRatio", stats.hitRatio);
                        cacheMap.put("size", stats.size);
                        return cacheMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("caches", cacheList);
            cacheData.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/cache", cacheData);
            
            log.debug("Broadcasted cache metrics to subscribers");
        } catch (Exception e) {
            log.error("Error broadcasting cache metrics", e);
        }
    }

    /**
     * Broadcast combined metrics summary every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void broadcastMetricsSummary() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }

        try {
            Map<String, Object> summary = new HashMap<>();
            
            // Health
            summary.put("healthScore", serviceHealthService.getOverallHealthScore());
            
            // Throttling
            summary.put("throttlingRate", metricsService.getCurrentThrottlingRate());
            
            // Cache
            Set<String> cacheNames = cacheMetricsService.getMonitoredCacheNames();
            double avgHitRatio = cacheNames.stream()
                    .mapToDouble(cacheMetricsService::getCacheHitRatio)
                    .average()
                    .orElse(0.0);
            summary.put("avgCacheHitRatio", avgHitRatio);
            
            summary.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/metrics-summary", summary);
            
            log.debug("Broadcasted metrics summary to subscribers");
        } catch (Exception e) {
            log.error("Error broadcasting metrics summary", e);
        }
    }

    /**
     * Broadcast alert when health score drops below threshold
     */
    public void broadcastHealthAlert(int healthScore, String message) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "health");
            alert.put("severity", healthScore < 50 ? "critical" : "warning");
            alert.put("healthScore", healthScore);
            alert.put("message", message);
            alert.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            
            log.info("Broadcasted health alert: score={}, message={}", healthScore, message);
        } catch (Exception e) {
            log.error("Error broadcasting health alert", e);
        }
    }

    /**
     * Broadcast alert when throttling rate exceeds threshold
     */
    public void broadcastThrottlingAlert(double throttlingRate, String message) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "throttling");
            alert.put("severity", throttlingRate > 15 ? "critical" : "warning");
            alert.put("throttlingRate", throttlingRate);
            alert.put("message", message);
            alert.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            
            log.info("Broadcasted throttling alert: rate={}, message={}", throttlingRate, message);
        } catch (Exception e) {
            log.error("Error broadcasting throttling alert", e);
        }
    }

    /**
     * Broadcast alert when cache hit ratio drops below threshold
     */
    public void broadcastCacheAlert(String cacheName, double hitRatio, String message) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "cache");
            alert.put("severity", hitRatio < 0.6 ? "critical" : "warning");
            alert.put("cacheName", cacheName);
            alert.put("hitRatio", hitRatio);
            alert.put("message", message);
            alert.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            
            log.info("Broadcasted cache alert: cache={}, hitRatio={}, message={}", 
                    cacheName, hitRatio, message);
        } catch (Exception e) {
            log.error("Error broadcasting cache alert", e);
        }
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
            log.debug("Sent message to user: {}, destination: {}", username, destination);
        } catch (Exception e) {
            log.error("Error sending message to user: {}", username, e);
        }
    }
}
