package com.personal.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {
    
    /**
     * Enable/disable custom metrics collection
     */
    private boolean enabled = true;
    
    /**
     * Metrics collection interval
     */
    private Duration interval = Duration.ofSeconds(15);
    
    /**
     * Cache metrics configuration
     */
    private Cache cache = new Cache();
    
    /**
     * Throttling metrics configuration
     */
    private Throttling throttling = new Throttling();
    
    /**
     * Health check configuration
     */
    private HealthCheck healthCheck = new HealthCheck();
    
    /**
     * Alert thresholds configuration
     */
    private Alerts alerts = new Alerts();

    @Data
    public static class Cache {
        private boolean enabled = true;
        private Duration updateInterval = Duration.ofSeconds(30);
        private Map<String, Double> hitRatioThresholds = Map.of(
            "default", 0.8,
            "api-cache", 0.85,
            "database-cache", 0.9
        );
    }

    @Data
    public static class Throttling {
        private boolean enabled = true;
        private Duration windowSize = Duration.ofMinutes(5);
        private double warningThreshold = 0.05; // 5%
        private double criticalThreshold = 0.15; // 15%
    }

    @Data
    public static class HealthCheck {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(30);
        private Duration timeout = Duration.ofSeconds(10);
        private int retryAttempts = 3;
    }

    @Data
    public static class Alerts {
        private ResponseTime responseTime = new ResponseTime();
        private ErrorRate errorRate = new ErrorRate();
        private Availability availability = new Availability();

        @Data
        public static class ResponseTime {
            private Duration p95Warning = Duration.ofSeconds(2);
            private Duration p95Critical = Duration.ofSeconds(5);
            private Duration p99Critical = Duration.ofSeconds(10);
        }

        @Data
        public static class ErrorRate {
            private double warningThreshold = 0.01; // 1%
            private double criticalThreshold = 0.05; // 5%
            private Duration evaluationWindow = Duration.ofMinutes(5);
        }

        @Data
        public static class Availability {
            private double warningThreshold = 0.99; // 99%
            private double criticalThreshold = 0.95; // 95%
            private Duration evaluationWindow = Duration.ofMinutes(10);
        }
    }
}