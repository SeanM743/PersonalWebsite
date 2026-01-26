package com.personal.backend.service;

import com.personal.backend.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final HistoricalDataService historicalDataService;
    private final PrometheusQueryService prometheusQueryService;
    private final MonitoringProperties monitoringProperties;

    /**
     * Generate SLA report for a time period
     */
    public Map<String, Object> generateSLAReport(int days) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", "SLA");
            report.put("period", days + " days");
            report.put("generatedAt", Instant.now().toString());
            
            // Availability metrics
            Map<String, Object> availability = calculateAvailability(days);
            report.put("availability", availability);
            
            // Performance metrics
            Map<String, Object> performance = calculatePerformance(days);
            report.put("performance", performance);
            
            // Error metrics
            Map<String, Object> errors = calculateErrorMetrics(days);
            report.put("errors", errors);
            
            // SLA compliance
            Map<String, Object> compliance = calculateSLACompliance(availability, performance, errors);
            report.put("compliance", compliance);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating SLA report", e);
            return Map.of("error", "Failed to generate SLA report", "message", e.getMessage());
        }
    }

    /**
     * Generate performance summary report
     */
    public Map<String, Object> generatePerformanceSummary(int days) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", "Performance Summary");
            report.put("period", days + " days");
            report.put("generatedAt", Instant.now().toString());
            
            // Response time statistics
            Map<String, Object> responseTimeStats = historicalDataService.getAggregatedStats("response_time_p95", days);
            report.put("responseTime", responseTimeStats);
            
            // Throttling statistics
            Map<String, Object> throttlingStats = historicalDataService.getAggregatedStats("throttling_rate", days);
            report.put("throttling", throttlingStats);
            
            // Health score statistics
            Map<String, Object> healthStats = historicalDataService.getAggregatedStats("health_score", days);
            report.put("healthScore", healthStats);
            
            // Trends
            Map<String, Object> trends = calculateTrends(days);
            report.put("trends", trends);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating performance summary", e);
            return Map.of("error", "Failed to generate performance summary", "message", e.getMessage());
        }
    }

    /**
     * Generate cache performance report
     */
    public Map<String, Object> generateCacheReport(int days) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", "Cache Performance");
            report.put("period", days + " days");
            report.put("generatedAt", Instant.now().toString());
            
            // Cache hit ratio trends
            List<HistoricalDataService.HistoricalDataPoint> cacheData = 
                    historicalDataService.getCacheHitRatioHistory(null, days);
            
            DoubleSummaryStatistics stats = cacheData.stream()
                    .mapToDouble(dp -> dp.value)
                    .summaryStatistics();
            
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("averageHitRatio", stats.getAverage());
            cacheStats.put("minHitRatio", stats.getMin());
            cacheStats.put("maxHitRatio", stats.getMax());
            cacheStats.put("dataPoints", stats.getCount());
            
            report.put("cacheStatistics", cacheStats);
            
            // Cache efficiency score
            double efficiencyScore = calculateCacheEfficiency(stats.getAverage());
            report.put("efficiencyScore", efficiencyScore);
            report.put("efficiencyRating", getEfficiencyRating(efficiencyScore));
            
            return report;
        } catch (Exception e) {
            log.error("Error generating cache report", e);
            return Map.of("error", "Failed to generate cache report", "message", e.getMessage());
        }
    }

    /**
     * Generate daily summary report
     */
    public Map<String, Object> generateDailySummary() {
        return generateSummaryReport(1, "Daily");
    }

    /**
     * Generate weekly summary report
     */
    public Map<String, Object> generateWeeklySummary() {
        return generateSummaryReport(7, "Weekly");
    }

    /**
     * Generate monthly summary report
     */
    public Map<String, Object> generateMonthlySummary() {
        return generateSummaryReport(30, "Monthly");
    }

    /**
     * Generate custom summary report
     */
    private Map<String, Object> generateSummaryReport(int days, String reportName) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", reportName + " Summary");
            report.put("period", days + " days");
            report.put("generatedAt", Instant.now().toString());
            
            // Current status
            Map<String, Object> currentStatus = new HashMap<>();
            currentStatus.put("healthScore", prometheusQueryService.getHealthScore());
            currentStatus.put("throttlingRate", prometheusQueryService.getThrottlingRate());
            currentStatus.put("responseTimeP95", prometheusQueryService.getResponseTimePercentile(0.95, null));
            report.put("currentStatus", currentStatus);
            
            // Historical trends
            Map<String, Object> trends = calculateTrends(days);
            report.put("trends", trends);
            
            // Key metrics
            Map<String, Object> keyMetrics = new HashMap<>();
            keyMetrics.put("healthScore", historicalDataService.getAggregatedStats("health_score", days));
            keyMetrics.put("throttlingRate", historicalDataService.getAggregatedStats("throttling_rate", days));
            keyMetrics.put("responseTime", historicalDataService.getAggregatedStats("response_time_p95", days));
            report.put("keyMetrics", keyMetrics);
            
            // Recommendations
            List<String> recommendations = generateRecommendations(currentStatus, trends);
            report.put("recommendations", recommendations);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating {} summary", reportName, e);
            return Map.of("error", "Failed to generate summary", "message", e.getMessage());
        }
    }

    /**
     * Format report as text
     */
    public String formatReportAsText(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=".repeat(60)).append("\n");
        sb.append(report.get("reportType")).append("\n");
        sb.append("Period: ").append(report.get("period")).append("\n");
        sb.append("Generated: ").append(report.get("generatedAt")).append("\n");
        sb.append("=".repeat(60)).append("\n\n");
        
        // Add report sections
        report.forEach((key, value) -> {
            if (!key.equals("reportType") && !key.equals("period") && !key.equals("generatedAt")) {
                sb.append(formatSection(key, value));
            }
        });
        
        return sb.toString();
    }

    private Map<String, Object> calculateAvailability(int days) {
        // Calculate uptime percentage
        List<HistoricalDataService.HistoricalDataPoint> healthData = 
                historicalDataService.getHealthScoreHistory(days);
        
        long totalPoints = healthData.size();
        long healthyPoints = healthData.stream()
                .filter(dp -> dp.value >= 80)
                .count();
        
        double availability = totalPoints > 0 ? (double) healthyPoints / totalPoints * 100 : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("percentage", availability);
        result.put("totalDataPoints", totalPoints);
        result.put("healthyDataPoints", healthyPoints);
        result.put("target", 99.9);
        result.put("met", availability >= 99.0);
        
        return result;
    }

    private Map<String, Object> calculatePerformance(int days) {
        Map<String, Object> responseTimeStats = historicalDataService.getAggregatedStats("response_time_p95", days);
        
        double avgResponseTime = (double) responseTimeStats.getOrDefault("average", 0.0);
        double maxResponseTime = (double) responseTimeStats.getOrDefault("max", 0.0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("averageResponseTime", avgResponseTime);
        result.put("maxResponseTime", maxResponseTime);
        result.put("target", 2.0); // 2 seconds
        result.put("met", avgResponseTime < 2.0);
        
        return result;
    }

    private Map<String, Object> calculateErrorMetrics(int days) {
        // Simplified error calculation
        Map<String, Object> result = new HashMap<>();
        result.put("errorRate", 0.01); // Placeholder
        result.put("target", 0.01); // 1%
        result.put("met", true);
        
        return result;
    }

    private Map<String, Object> calculateSLACompliance(Map<String, Object> availability, 
                                                       Map<String, Object> performance, 
                                                       Map<String, Object> errors) {
        boolean availabilityMet = (boolean) availability.get("met");
        boolean performanceMet = (boolean) performance.get("met");
        boolean errorsMet = (boolean) errors.get("met");
        
        int totalCriteria = 3;
        int metCriteria = (availabilityMet ? 1 : 0) + (performanceMet ? 1 : 0) + (errorsMet ? 1 : 0);
        
        double compliancePercentage = (double) metCriteria / totalCriteria * 100;
        
        Map<String, Object> result = new HashMap<>();
        result.put("overallCompliance", compliancePercentage);
        result.put("availabilityMet", availabilityMet);
        result.put("performanceMet", performanceMet);
        result.put("errorsMet", errorsMet);
        result.put("status", compliancePercentage >= 100 ? "COMPLIANT" : "NON_COMPLIANT");
        
        return result;
    }

    private Map<String, Object> calculateTrends(int days) {
        Map<String, Object> trends = new HashMap<>();
        
        // Health score trend
        List<HistoricalDataService.HistoricalDataPoint> healthData = 
                historicalDataService.getHealthScoreHistory(days);
        trends.put("healthScore", calculateTrend(healthData));
        
        // Throttling rate trend
        List<HistoricalDataService.HistoricalDataPoint> throttlingData = 
                historicalDataService.getThrottlingRateHistory(days);
        trends.put("throttlingRate", calculateTrend(throttlingData));
        
        // Response time trend
        List<HistoricalDataService.HistoricalDataPoint> responseTimeData = 
                historicalDataService.getResponseTimeHistory(0.95, days);
        trends.put("responseTime", calculateTrend(responseTimeData));
        
        return trends;
    }

    private String calculateTrend(List<HistoricalDataService.HistoricalDataPoint> data) {
        if (data.size() < 2) {
            return "STABLE";
        }
        
        int halfPoint = data.size() / 2;
        double firstHalfAvg = data.subList(0, halfPoint).stream()
                .mapToDouble(dp -> dp.value)
                .average()
                .orElse(0);
        double secondHalfAvg = data.subList(halfPoint, data.size()).stream()
                .mapToDouble(dp -> dp.value)
                .average()
                .orElse(0);
        
        double change = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;
        
        if (Math.abs(change) < 5) {
            return "STABLE";
        } else if (change > 0) {
            return "INCREASING";
        } else {
            return "DECREASING";
        }
    }

    private double calculateCacheEfficiency(double avgHitRatio) {
        return avgHitRatio * 100;
    }

    private String getEfficiencyRating(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 80) return "GOOD";
        if (score >= 70) return "FAIR";
        return "POOR";
    }

    private List<String> generateRecommendations(Map<String, Object> currentStatus, Map<String, Object> trends) {
        List<String> recommendations = new ArrayList<>();
        
        Double healthScore = (Double) currentStatus.get("healthScore");
        if (healthScore != null && healthScore < 80) {
            recommendations.add("Health score is below optimal. Review service health checks and address any failing services.");
        }
        
        Double throttlingRate = (Double) currentStatus.get("throttlingRate");
        if (throttlingRate != null && throttlingRate > 5) {
            recommendations.add("Throttling rate is elevated. Consider increasing rate limits or scaling resources.");
        }
        
        String healthTrend = (String) trends.get("healthScore");
        if ("DECREASING".equals(healthTrend)) {
            recommendations.add("Health score is trending downward. Investigate potential issues before they become critical.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("All metrics are within acceptable ranges. Continue monitoring.");
        }
        
        return recommendations;
    }

    private String formatSection(String key, Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append(key.toUpperCase().replace("_", " ")).append(":\n");
        sb.append("-".repeat(40)).append("\n");
        
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            map.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        } else {
            sb.append("  ").append(value).append("\n");
        }
        
        sb.append("\n");
        return sb.toString();
    }
}
