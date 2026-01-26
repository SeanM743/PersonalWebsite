package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataService {

    private final PrometheusQueryService prometheusQueryService;
    private final MonitoringProperties monitoringProperties;
    
    // In-memory cache for aggregated historical data
    private final ConcurrentHashMap<String, List<HistoricalDataPoint>> historicalCache = new ConcurrentHashMap<>();
    
    // Retention periods in days
    private static final int SHORT_TERM_RETENTION = 7;   // 7 days
    private static final int MEDIUM_TERM_RETENTION = 30;  // 30 days
    private static final int LONG_TERM_RETENTION = 90;    // 90 days

    /**
     * Get historical health score data
     */
    public List<HistoricalDataPoint> getHealthScoreHistory(int days) {
        String cacheKey = "health_score_" + days;
        
        if (historicalCache.containsKey(cacheKey)) {
            List<HistoricalDataPoint> cached = historicalCache.get(cacheKey);
            if (!cached.isEmpty() && isDataFresh(cached.get(cached.size() - 1).timestamp, 300)) {
                return cached;
            }
        }
        
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        
        List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                "service_health_score", startTime, endTime, calculateStep(days));
        
        List<HistoricalDataPoint> dataPoints = convertToDataPoints(timeSeries);
        historicalCache.put(cacheKey, dataPoints);
        
        return dataPoints;
    }

    /**
     * Get historical throttling rate data
     */
    public List<HistoricalDataPoint> getThrottlingRateHistory(int days) {
        String cacheKey = "throttling_rate_" + days;
        
        if (historicalCache.containsKey(cacheKey)) {
            List<HistoricalDataPoint> cached = historicalCache.get(cacheKey);
            if (!cached.isEmpty() && isDataFresh(cached.get(cached.size() - 1).timestamp, 300)) {
                return cached;
            }
        }
        
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        
        List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                "throttling_rate", startTime, endTime, calculateStep(days));
        
        List<HistoricalDataPoint> dataPoints = convertToDataPoints(timeSeries);
        historicalCache.put(cacheKey, dataPoints);
        
        return dataPoints;
    }

    /**
     * Get historical cache hit ratio data
     */
    public List<HistoricalDataPoint> getCacheHitRatioHistory(String cacheName, int days) {
        String cacheKey = "cache_hit_ratio_" + cacheName + "_" + days;
        
        if (historicalCache.containsKey(cacheKey)) {
            List<HistoricalDataPoint> cached = historicalCache.get(cacheKey);
            if (!cached.isEmpty() && isDataFresh(cached.get(cached.size() - 1).timestamp, 300)) {
                return cached;
            }
        }
        
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        
        String query = cacheName != null
                ? String.format("cache_hit_ratio{cache_name=\"%s\"}", cacheName)
                : "cache_hit_ratio";
        
        List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                query, startTime, endTime, calculateStep(days));
        
        List<HistoricalDataPoint> dataPoints = convertToDataPoints(timeSeries);
        historicalCache.put(cacheKey, dataPoints);
        
        return dataPoints;
    }

    /**
     * Get historical response time data
     */
    public List<HistoricalDataPoint> getResponseTimeHistory(double percentile, int days) {
        String cacheKey = "response_time_p" + (int)(percentile * 100) + "_" + days;
        
        if (historicalCache.containsKey(cacheKey)) {
            List<HistoricalDataPoint> cached = historicalCache.get(cacheKey);
            if (!cached.isEmpty() && isDataFresh(cached.get(cached.size() - 1).timestamp, 300)) {
                return cached;
            }
        }
        
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        
        String query = String.format("histogram_quantile(%.2f, rate(http_request_duration_seconds_bucket[5m]))", percentile);
        
        List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                query, startTime, endTime, calculateStep(days));
        
        List<HistoricalDataPoint> dataPoints = convertToDataPoints(timeSeries);
        historicalCache.put(cacheKey, dataPoints);
        
        return dataPoints;
    }

    /**
     * Get aggregated statistics for a time period
     */
    public Map<String, Object> getAggregatedStats(String metricName, int days) {
        List<HistoricalDataPoint> data;
        
        switch (metricName) {
            case "health_score":
                data = getHealthScoreHistory(days);
                break;
            case "throttling_rate":
                data = getThrottlingRateHistory(days);
                break;
            case "response_time_p95":
                data = getResponseTimeHistory(0.95, days);
                break;
            default:
                return Map.of("error", "Unknown metric: " + metricName);
        }
        
        if (data.isEmpty()) {
            return Map.of("error", "No data available");
        }
        
        DoubleSummaryStatistics stats = data.stream()
                .mapToDouble(dp -> dp.value)
                .summaryStatistics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("metric", metricName);
        result.put("days", days);
        result.put("count", stats.getCount());
        result.put("min", stats.getMin());
        result.put("max", stats.getMax());
        result.put("average", stats.getAverage());
        result.put("sum", stats.getSum());
        
        return result;
    }

    /**
     * Clean up old cached data (scheduled task)
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldCache() {
        if (!monitoringProperties.isEnabled()) {
            return;
        }
        
        try {
            long now = Instant.now().getEpochSecond();
            int removed = 0;
            
            for (Map.Entry<String, List<HistoricalDataPoint>> entry : historicalCache.entrySet()) {
                List<HistoricalDataPoint> dataPoints = entry.getValue();
                if (!dataPoints.isEmpty()) {
                    HistoricalDataPoint lastPoint = dataPoints.get(dataPoints.size() - 1);
                    // Remove cache entries older than 1 hour
                    if (now - lastPoint.timestamp > 3600) {
                        historicalCache.remove(entry.getKey());
                        removed++;
                    }
                }
            }
            
            if (removed > 0) {
                log.info("Cleaned up {} old cache entries from historical data", removed);
            }
        } catch (Exception e) {
            log.error("Error cleaning up historical data cache", e);
        }
    }

    /**
     * Get data retention configuration
     */
    public Map<String, Object> getRetentionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("shortTermDays", SHORT_TERM_RETENTION);
        config.put("mediumTermDays", MEDIUM_TERM_RETENTION);
        config.put("longTermDays", LONG_TERM_RETENTION);
        config.put("prometheusRetention", "30d"); // From prometheus.yml
        config.put("cacheEnabled", true);
        config.put("cacheSize", historicalCache.size());
        
        return config;
    }

    /**
     * Export historical data for a metric
     */
    public List<Map<String, Object>> exportHistoricalData(String metricName, int days) {
        List<HistoricalDataPoint> data;
        
        switch (metricName) {
            case "health_score":
                data = getHealthScoreHistory(days);
                break;
            case "throttling_rate":
                data = getThrottlingRateHistory(days);
                break;
            case "response_time_p95":
                data = getResponseTimeHistory(0.95, days);
                break;
            case "response_time_p99":
                data = getResponseTimeHistory(0.99, days);
                break;
            default:
                return Collections.emptyList();
        }
        
        List<Map<String, Object>> export = new ArrayList<>();
        for (HistoricalDataPoint point : data) {
            Map<String, Object> row = new HashMap<>();
            row.put("timestamp", point.timestamp);
            row.put("datetime", Instant.ofEpochSecond(point.timestamp).toString());
            row.put("value", point.value);
            row.put("metric", metricName);
            export.add(row);
        }
        
        return export;
    }

    private List<HistoricalDataPoint> convertToDataPoints(List<Map<String, Object>> timeSeries) {
        List<HistoricalDataPoint> dataPoints = new ArrayList<>();
        
        for (Map<String, Object> point : timeSeries) {
            Long timestamp = (Long) point.get("timestamp");
            Double value = (Double) point.get("value");
            
            if (timestamp != null && value != null) {
                dataPoints.add(new HistoricalDataPoint(timestamp, value));
            }
        }
        
        return dataPoints;
    }

    private String calculateStep(int days) {
        if (days <= 1) {
            return "1m";  // 1 minute for 1 day
        } else if (days <= 7) {
            return "5m";  // 5 minutes for 1 week
        } else if (days <= 30) {
            return "15m"; // 15 minutes for 1 month
        } else {
            return "1h";  // 1 hour for longer periods
        }
    }

    private boolean isDataFresh(long timestamp, int maxAgeSeconds) {
        long now = Instant.now().getEpochSecond();
        return (now - timestamp) < maxAgeSeconds;
    }

    public static class HistoricalDataPoint {
        public final long timestamp;
        public final double value;

        public HistoricalDataPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}
