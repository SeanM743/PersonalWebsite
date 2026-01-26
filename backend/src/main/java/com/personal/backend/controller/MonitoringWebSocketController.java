package com.personal.backend.controller;

import com.personal.backend.service.MetricsBroadcastService;
import com.personal.backend.service.ServiceHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MonitoringWebSocketController {

    private final ServiceHealthService serviceHealthService;
    private final MetricsBroadcastService metricsBroadcastService;

    /**
     * Handle subscription to health metrics
     */
    @SubscribeMapping("/health")
    public Map<String, Object> subscribeToHealth() {
        log.info("Client subscribed to health metrics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to health metrics");
        response.put("healthScore", serviceHealthService.getOverallHealthScore());
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Handle subscription to throttling metrics
     */
    @SubscribeMapping("/throttling")
    public Map<String, Object> subscribeToThrottling() {
        log.info("Client subscribed to throttling metrics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to throttling metrics");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Handle subscription to cache metrics
     */
    @SubscribeMapping("/cache")
    public Map<String, Object> subscribeToCache() {
        log.info("Client subscribed to cache metrics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to cache metrics");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Handle subscription to alerts
     */
    @SubscribeMapping("/alerts")
    public Map<String, Object> subscribeToAlerts() {
        log.info("Client subscribed to alerts");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to alerts");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Handle client ping to keep connection alive
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing(Map<String, Object> message) {
        log.debug("Received ping from client");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Handle client request for immediate metrics update
     */
    @MessageMapping("/refresh")
    public void handleRefreshRequest() {
        log.info("Client requested immediate metrics refresh");
        
        // Trigger immediate broadcast
        metricsBroadcastService.broadcastHealthMetrics();
        metricsBroadcastService.broadcastThrottlingMetrics();
        metricsBroadcastService.broadcastCacheMetrics();
        metricsBroadcastService.broadcastMetricsSummary();
    }
}
