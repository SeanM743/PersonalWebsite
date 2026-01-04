package com.personal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PerformanceMonitoringService {
    
    private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> operationTotalTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> operationErrors = new ConcurrentHashMap<>();
    
    private static final long SLOW_OPERATION_THRESHOLD = 1000; // 1 second
    
    public void recordOperation(String operationName, long durationMs, boolean success) {
        operationCounts.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();
        operationTotalTime.computeIfAbsent(operationName, k -> new AtomicLong(0)).addAndGet(durationMs);
        
        if (!success) {
            operationErrors.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        if (durationMs > SLOW_OPERATION_THRESHOLD) {
            log.warn("Slow operation detected: {} took {}ms", operationName, durationMs);
        }
        
        log.debug("Operation {} completed in {}ms (success: {})", operationName, durationMs, success);
    }
    
    @Cacheable(value = "performance-stats", unless = "#result == null")
    public Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        for (String operation : operationCounts.keySet()) {
            long count = operationCounts.get(operation).get();
            long totalTime = operationTotalTime.get(operation).get();
            long errors = operationErrors.getOrDefault(operation, new AtomicLong(0)).get();
            
            double avgTime = count > 0 ? (double) totalTime / count : 0;
            double errorRate = count > 0 ? (double) errors / count * 100 : 0;
            
            Map<String, Object> operationStats = Map.of(
                    "totalCalls", count,
                    "totalTimeMs", totalTime,
                    "averageTimeMs", Math.round(avgTime * 100.0) / 100.0,
                    "errorCount", errors,
                    "errorRate", Math.round(errorRate * 100.0) / 100.0
            );
            
            stats.put(operation, operationStats);
        }
        
        stats.put("generatedAt", LocalDateTime.now());
        return stats;
    }
    
    public void resetStatistics() {
        operationCounts.clear();
        operationTotalTime.clear();
        operationErrors.clear();
        log.info("Performance statistics reset");
    }
    
    public Map<String, Object> getSlowOperations() {
        Map<String, Object> slowOps = new ConcurrentHashMap<>();
        
        for (String operation : operationCounts.keySet()) {
            long count = operationCounts.get(operation).get();
            long totalTime = operationTotalTime.get(operation).get();
            
            if (count > 0) {
                double avgTime = (double) totalTime / count;
                if (avgTime > SLOW_OPERATION_THRESHOLD) {
                    slowOps.put(operation, Map.of(
                            "averageTimeMs", Math.round(avgTime * 100.0) / 100.0,
                            "totalCalls", count,
                            "threshold", SLOW_OPERATION_THRESHOLD
                    ));
                }
            }
        }
        
        return slowOps;
    }
    
    public void logPerformanceSummary() {
        Map<String, Object> stats = getPerformanceStatistics();
        log.info("Performance Summary: {}", stats);
        
        Map<String, Object> slowOps = getSlowOperations();
        if (!slowOps.isEmpty()) {
            log.warn("Slow Operations Detected: {}", slowOps);
        }
    }
}