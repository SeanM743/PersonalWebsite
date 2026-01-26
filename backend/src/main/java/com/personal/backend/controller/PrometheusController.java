package com.personal.backend.controller;

import com.personal.backend.service.PrometheusQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PrometheusController {

    private final PrometheusQueryService prometheusQueryService;

    /**
     * Execute instant query
     */
    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @RequestParam String query,
            @RequestParam(required = false) Long time) {
        try {
            Map<String, Object> result = prometheusQueryService.queryInstant(query, time);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing Prometheus query", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to execute query", "message", e.getMessage()));
        }
    }

    /**
     * Execute range query
     */
    @GetMapping("/query_range")
    public ResponseEntity<Map<String, Object>> queryRange(
            @RequestParam String query,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String step) {
        try {
            Map<String, Object> result = prometheusQueryService.queryRange(query, start, end, step);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing Prometheus range query", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to execute query", "message", e.getMessage()));
        }
    }

    /**
     * Get health score time series
     */
    @GetMapping("/metrics/health-score")
    public ResponseEntity<Map<String, Object>> getHealthScoreTimeSeries(
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    "service_health_score", startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "service_health_score");
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting health score time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get health score", "message", e.getMessage()));
        }
    }

    /**
     * Get throttling rate time series
     */
    @GetMapping("/metrics/throttling-rate")
    public ResponseEntity<Map<String, Object>> getThrottlingRateTimeSeries(
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    "throttling_rate", startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "throttling_rate");
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting throttling rate time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get throttling rate", "message", e.getMessage()));
        }
    }

    /**
     * Get cache hit ratio time series
     */
    @GetMapping("/metrics/cache-hit-ratio")
    public ResponseEntity<Map<String, Object>> getCacheHitRatioTimeSeries(
            @RequestParam(required = false) String cacheName,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            String query = cacheName != null
                    ? String.format("cache_hit_ratio{cache_name=\"%s\"}", cacheName)
                    : "cache_hit_ratio";
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    query, startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "cache_hit_ratio");
            response.put("cacheName", cacheName);
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting cache hit ratio time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get cache hit ratio", "message", e.getMessage()));
        }
    }

    /**
     * Get response time percentiles time series
     */
    @GetMapping("/metrics/response-time")
    public ResponseEntity<Map<String, Object>> getResponseTimeTimeSeries(
            @RequestParam(required = false) String endpoint,
            @RequestParam(defaultValue = "0.95") double percentile,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            String query = endpoint != null
                    ? String.format("histogram_quantile(%.2f, rate(http_request_duration_seconds_bucket{endpoint=\"%s\"}[5m]))", 
                            percentile, endpoint)
                    : String.format("histogram_quantile(%.2f, rate(http_request_duration_seconds_bucket[5m]))", percentile);
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    query, startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "response_time");
            response.put("percentile", percentile);
            response.put("endpoint", endpoint);
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting response time time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get response time", "message", e.getMessage()));
        }
    }

    /**
     * Get error rate time series
     */
    @GetMapping("/metrics/error-rate")
    public ResponseEntity<Map<String, Object>> getErrorRateTimeSeries(
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            String query = endpoint != null
                    ? String.format("rate(http_errors_total{endpoint=\"%s\"}[5m])", endpoint)
                    : "rate(http_errors_total[5m])";
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    query, startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "error_rate");
            response.put("endpoint", endpoint);
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting error rate time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get error rate", "message", e.getMessage()));
        }
    }

    /**
     * Get request rate time series
     */
    @GetMapping("/metrics/request-rate")
    public ResponseEntity<Map<String, Object>> getRequestRateTimeSeries(
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "15s") String step) {
        try {
            Long startTime = start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond();
            Long endTime = end != null ? end : Instant.now().getEpochSecond();
            
            String query = endpoint != null
                    ? String.format("rate(http_requests_total{endpoint=\"%s\"}[5m])", endpoint)
                    : "rate(http_requests_total[5m])";
            
            List<Map<String, Object>> timeSeries = prometheusQueryService.getMetricTimeSeries(
                    query, startTime, endTime, step);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metric", "request_rate");
            response.put("endpoint", endpoint);
            response.put("data", timeSeries);
            response.put("start", startTime);
            response.put("end", endTime);
            response.put("step", step);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting request rate time series", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get request rate", "message", e.getMessage()));
        }
    }

    /**
     * Get dashboard data (combined metrics)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Current values
            response.put("healthScore", prometheusQueryService.getHealthScore());
            response.put("throttlingRate", prometheusQueryService.getThrottlingRate());
            response.put("responseTimeP95", prometheusQueryService.getResponseTimePercentile(0.95, null));
            response.put("responseTimeP99", prometheusQueryService.getResponseTimePercentile(0.99, null));
            response.put("errorRate", prometheusQueryService.getErrorRate(null));
            response.put("requestRate", prometheusQueryService.getRequestRate(null));
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting dashboard data", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get dashboard data", "message", e.getMessage()));
        }
    }
}
