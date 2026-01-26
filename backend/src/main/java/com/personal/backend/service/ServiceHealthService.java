package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ServiceHealthService implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final MonitoringProperties monitoringProperties;
    private final DataSource dataSource;
    
    // Health status tracking
    private final ConcurrentHashMap<String, ServiceHealthStatus> serviceHealthMap = new ConcurrentHashMap<>();
    private final AtomicInteger overallHealthScore = new AtomicInteger(100);
    private final AtomicLong lastHealthCheckTime = new AtomicLong(0);
    
    // Health check counters
    private final Counter healthCheckCounter;
    private final Counter healthCheckFailureCounter;
    private final Gauge overallHealthGauge;

    public ServiceHealthService(MeterRegistry meterRegistry, MonitoringProperties monitoringProperties, DataSource dataSource) {
        this.meterRegistry = meterRegistry;
        this.monitoringProperties = monitoringProperties;
        this.dataSource = dataSource;
        
        // Initialize metrics
        this.healthCheckCounter = Counter.builder("health_checks_total")
                .description("Total health checks performed")
                .register(meterRegistry);
                
        this.healthCheckFailureCounter = Counter.builder("health_check_failures_total")
                .description("Total health check failures")
                .register(meterRegistry);
                
        this.overallHealthGauge = Gauge.builder("service_health_score", this, ServiceHealthService::getOverallHealthScore)
                .description("Overall service health score (0-100)")
                .register(meterRegistry);
        
        // Initialize service health statuses
        initializeServiceHealth();
    }

    /**
     * Scheduled health check execution
     */
    @Scheduled(fixedRateString = "${monitoring.health-check.interval:30000}")
    public void performScheduledHealthChecks() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        log.debug("Performing scheduled health checks");
        
        try {
            checkDatabaseHealth();
            checkMemoryHealth();
            checkDiskSpaceHealth();
            checkExternalServicesHealth();
            
            updateOverallHealthScore();
            lastHealthCheckTime.set(Instant.now().toEpochMilli());
            
            healthCheckCounter.increment();
            
        } catch (Exception e) {
            log.error("Error during scheduled health check", e);
            healthCheckFailureCounter.increment();
        }
    }

    /**
     * Check database connectivity and performance
     */
    public ServiceHealthStatus checkDatabaseHealth() {
        String serviceName = "database";
        Instant startTime = Instant.now();
        
        try {
            try (Connection connection = dataSource.getConnection()) {
                // Test connection with a simple query
                boolean isValid = connection.isValid(5); // 5 second timeout
                
                Duration responseTime = Duration.between(startTime, Instant.now());
                
                if (isValid && responseTime.toMillis() < 1000) {
                    return updateServiceHealth(serviceName, HealthStatus.UP, "Database connection healthy", responseTime);
                } else if (isValid) {
                    return updateServiceHealth(serviceName, HealthStatus.DEGRADED, "Database connection slow", responseTime);
                } else {
                    return updateServiceHealth(serviceName, HealthStatus.DOWN, "Database connection invalid", responseTime);
                }
            }
        } catch (SQLException e) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            log.error("Database health check failed", e);
            return updateServiceHealth(serviceName, HealthStatus.DOWN, "Database connection failed: " + e.getMessage(), responseTime);
        }
    }

    /**
     * Check memory usage health
     */
    public ServiceHealthStatus checkMemoryHealth() {
        String serviceName = "memory";
        Instant startTime = Instant.now();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            if (memoryUsagePercent < 70) {
                return updateServiceHealth(serviceName, HealthStatus.UP, 
                    String.format("Memory usage: %.1f%%", memoryUsagePercent), responseTime);
            } else if (memoryUsagePercent < 85) {
                return updateServiceHealth(serviceName, HealthStatus.DEGRADED, 
                    String.format("High memory usage: %.1f%%", memoryUsagePercent), responseTime);
            } else {
                return updateServiceHealth(serviceName, HealthStatus.DOWN, 
                    String.format("Critical memory usage: %.1f%%", memoryUsagePercent), responseTime);
            }
        } catch (Exception e) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            log.error("Memory health check failed", e);
            return updateServiceHealth(serviceName, HealthStatus.DOWN, "Memory check failed: " + e.getMessage(), responseTime);
        }
    }

    /**
     * Check disk space health
     */
    public ServiceHealthStatus checkDiskSpaceHealth() {
        String serviceName = "disk_space";
        Instant startTime = Instant.now();
        
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double diskUsagePercent = (double) usedSpace / totalSpace * 100;
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            if (diskUsagePercent < 80) {
                return updateServiceHealth(serviceName, HealthStatus.UP, 
                    String.format("Disk usage: %.1f%%", diskUsagePercent), responseTime);
            } else if (diskUsagePercent < 90) {
                return updateServiceHealth(serviceName, HealthStatus.DEGRADED, 
                    String.format("High disk usage: %.1f%%", diskUsagePercent), responseTime);
            } else {
                return updateServiceHealth(serviceName, HealthStatus.DOWN, 
                    String.format("Critical disk usage: %.1f%%", diskUsagePercent), responseTime);
            }
        } catch (Exception e) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            log.error("Disk space health check failed", e);
            return updateServiceHealth(serviceName, HealthStatus.DOWN, "Disk check failed: " + e.getMessage(), responseTime);
        }
    }

    /**
     * Check external services health (placeholder)
     */
    public ServiceHealthStatus checkExternalServicesHealth() {
        String serviceName = "external_services";
        Instant startTime = Instant.now();
        
        try {
            // Placeholder for external service checks
            // In a real implementation, you would check APIs, message queues, etc.
            
            Duration responseTime = Duration.between(startTime, Instant.now());
            return updateServiceHealth(serviceName, HealthStatus.UP, "External services healthy", responseTime);
            
        } catch (Exception e) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            log.error("External services health check failed", e);
            return updateServiceHealth(serviceName, HealthStatus.DOWN, "External services check failed: " + e.getMessage(), responseTime);
        }
    }

    /**
     * Get overall health status
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        try {
            int healthScore = getOverallHealthScore();
            builder.withDetail("overall_health_score", healthScore);
            builder.withDetail("last_check_time", Instant.ofEpochMilli(lastHealthCheckTime.get()));
            
            // Add individual service health details
            for (ServiceHealthStatus status : serviceHealthMap.values()) {
                builder.withDetail(status.serviceName + "_status", status.status.name());
                builder.withDetail(status.serviceName + "_message", status.message);
                builder.withDetail(status.serviceName + "_response_time_ms", status.responseTime.toMillis());
                builder.withDetail(status.serviceName + "_last_check", status.lastCheckTime);
            }
            
            // Determine overall status
            if (healthScore >= 80) {
                builder.up();
            } else if (healthScore >= 50) {
                builder.status(Status.OUT_OF_SERVICE);
            } else {
                builder.down();
            }
            
        } catch (Exception e) {
            log.error("Error during health check", e);
            builder.down().withDetail("error", e.getMessage());
        }
        
        return builder.build();
    }

    /**
     * Get service health status by name
     */
    public ServiceHealthStatus getServiceHealth(String serviceName) {
        return serviceHealthMap.get(serviceName);
    }

    /**
     * Get all service health statuses
     */
    public java.util.Collection<ServiceHealthStatus> getAllServiceHealth() {
        return serviceHealthMap.values();
    }

    /**
     * Get overall health score (0-100)
     */
    public int getOverallHealthScore() {
        return overallHealthScore.get();
    }

    private ServiceHealthStatus updateServiceHealth(String serviceName, HealthStatus status, String message, Duration responseTime) {
        ServiceHealthStatus healthStatus = new ServiceHealthStatus(
            serviceName, status, message, responseTime, Instant.now()
        );
        
        serviceHealthMap.put(serviceName, healthStatus);
        
        // Update metrics
        Counter.builder("service_health_checks_total")
                .description("Total health checks per service")
                .tags("service", serviceName, "status", status.name())
                .register(meterRegistry)
                .increment();
        
        Gauge.builder("service_health_response_time_ms", responseTime.toMillis(), value -> (double) value)
                .description("Service health check response time in milliseconds")
                .tags("service", serviceName)
                .register(meterRegistry);
        
        log.debug("Updated health status for service {}: {} - {}", serviceName, status, message);
        
        return healthStatus;
    }

    private void updateOverallHealthScore() {
        if (serviceHealthMap.isEmpty()) {
            overallHealthScore.set(100);
            return;
        }
        
        int totalScore = 0;
        int serviceCount = serviceHealthMap.size();
        
        for (ServiceHealthStatus status : serviceHealthMap.values()) {
            switch (status.status) {
                case UP:
                    totalScore += 100;
                    break;
                case DEGRADED:
                    totalScore += 60;
                    break;
                case DOWN:
                    totalScore += 0;
                    break;
                case UNKNOWN:
                default:
                    totalScore += 50; // Neutral score for unknown status
                    break;
            }
        }
        
        int averageScore = totalScore / serviceCount;
        overallHealthScore.set(averageScore);
    }

    private void initializeServiceHealth() {
        // Initialize with unknown status
        String[] services = {"database", "memory", "disk_space", "external_services"};
        
        for (String service : services) {
            serviceHealthMap.put(service, new ServiceHealthStatus(
                service, HealthStatus.UNKNOWN, "Not checked yet", Duration.ZERO, Instant.now()
            ));
        }
    }

    public enum HealthStatus {
        UP, DEGRADED, DOWN, UNKNOWN
    }

    public static class ServiceHealthStatus {
        public final String serviceName;
        public final HealthStatus status;
        public final String message;
        public final Duration responseTime;
        public final Instant lastCheckTime;

        public ServiceHealthStatus(String serviceName, HealthStatus status, String message, Duration responseTime, Instant lastCheckTime) {
            this.serviceName = serviceName;
            this.status = status;
            this.message = message;
            this.responseTime = responseTime;
            this.lastCheckTime = lastCheckTime;
        }
    }
}