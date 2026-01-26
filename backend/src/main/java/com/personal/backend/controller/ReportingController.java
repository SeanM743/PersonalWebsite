package com.personal.backend.controller;

import com.personal.backend.service.DataExportService;
import com.personal.backend.service.HistoricalDataService;
import com.personal.backend.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ReportingController {

    private final ReportingService reportingService;
    private final HistoricalDataService historicalDataService;
    private final DataExportService dataExportService;

    /**
     * Generate SLA report
     */
    @GetMapping("/sla")
    public ResponseEntity<Map<String, Object>> getSLAReport(
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> report = reportingService.generateSLAReport(days);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating SLA report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate SLA report", "message", e.getMessage()));
        }
    }

    /**
     * Generate performance summary report
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceReport(
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> report = reportingService.generatePerformanceSummary(days);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating performance report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate performance report", "message", e.getMessage()));
        }
    }

    /**
     * Generate cache performance report
     */
    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheReport(
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> report = reportingService.generateCacheReport(days);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating cache report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate cache report", "message", e.getMessage()));
        }
    }

    /**
     * Generate daily summary
     */
    @GetMapping("/summary/daily")
    public ResponseEntity<Map<String, Object>> getDailySummary() {
        try {
            Map<String, Object> report = reportingService.generateDailySummary();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating daily summary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate daily summary", "message", e.getMessage()));
        }
    }

    /**
     * Generate weekly summary
     */
    @GetMapping("/summary/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySummary() {
        try {
            Map<String, Object> report = reportingService.generateWeeklySummary();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating weekly summary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate weekly summary", "message", e.getMessage()));
        }
    }

    /**
     * Generate monthly summary
     */
    @GetMapping("/summary/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySummary() {
        try {
            Map<String, Object> report = reportingService.generateMonthlySummary();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating monthly summary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate monthly summary", "message", e.getMessage()));
        }
    }

    /**
     * Get report as text format
     */
    @GetMapping(value = "/sla/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getSLAReportText(
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> report = reportingService.generateSLAReport(days);
            String textReport = reportingService.formatReportAsText(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "sla-report.txt");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(textReport);
        } catch (Exception e) {
            log.error("Error generating SLA report text", e);
            return ResponseEntity.internalServerError()
                    .body("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Get historical data
     */
    @GetMapping("/historical/{metric}")
    public ResponseEntity<List<HistoricalDataService.HistoricalDataPoint>> getHistoricalData(
            @PathVariable String metric,
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<HistoricalDataService.HistoricalDataPoint> data;
            
            switch (metric) {
                case "health-score":
                    data = historicalDataService.getHealthScoreHistory(days);
                    break;
                case "throttling-rate":
                    data = historicalDataService.getThrottlingRateHistory(days);
                    break;
                case "response-time-p95":
                    data = historicalDataService.getResponseTimeHistory(0.95, days);
                    break;
                case "response-time-p99":
                    data = historicalDataService.getResponseTimeHistory(0.99, days);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting historical data for metric: {}", metric, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get data retention configuration
     */
    @GetMapping("/retention/config")
    public ResponseEntity<Map<String, Object>> getRetentionConfig() {
        try {
            Map<String, Object> config = historicalDataService.getRetentionConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error getting retention config", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get retention config", "message", e.getMessage()));
        }
    }

    /**
     * Export metric data as CSV
     */
    @GetMapping(value = "/export/{metric}/csv", produces = "text/csv")
    public ResponseEntity<String> exportMetricAsCSV(
            @PathVariable String metric,
            @RequestParam(defaultValue = "7") int days) {
        try {
            String csv = dataExportService.exportAsCSV(metric, days);
            String filename = dataExportService.getExportFilename(metric, "csv");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);
        } catch (Exception e) {
            log.error("Error exporting metric as CSV: {}", metric, e);
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Export metric data as JSON
     */
    @GetMapping(value = "/export/{metric}/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportMetricAsJSON(
            @PathVariable String metric,
            @RequestParam(defaultValue = "7") int days) {
        try {
            String json = dataExportService.exportAsJSON(metric, days);
            String filename = dataExportService.getExportFilename(metric, "json");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(json);
        } catch (Exception e) {
            log.error("Error exporting metric as JSON: {}", metric, e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Export multiple metrics as CSV
     */
    @PostMapping(value = "/export/bulk/csv", produces = "text/csv")
    public ResponseEntity<String> exportMultipleMetricsAsCSV(
            @RequestBody List<String> metrics,
            @RequestParam(defaultValue = "7") int days) {
        try {
            String csv = dataExportService.exportMultipleMetricsAsCSV(metrics, days);
            String filename = dataExportService.getExportFilename("bulk-export", "csv");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);
        } catch (Exception e) {
            log.error("Error exporting multiple metrics as CSV", e);
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Export report as JSON
     */
    @GetMapping(value = "/export/report/{reportType}/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportReportAsJSON(
            @PathVariable String reportType,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> report;
            
            switch (reportType) {
                case "sla":
                    report = reportingService.generateSLAReport(days);
                    break;
                case "performance":
                    report = reportingService.generatePerformanceSummary(days);
                    break;
                case "cache":
                    report = reportingService.generateCacheReport(days);
                    break;
                default:
                    return ResponseEntity.badRequest().body("{\"error\": \"Unknown report type\"}");
            }
            
            String json = dataExportService.exportReportAsJSON(report);
            String filename = dataExportService.getExportFilename(reportType + "-report", "json");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(json);
        } catch (Exception e) {
            log.error("Error exporting report as JSON: {}", reportType, e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Export report as CSV
     */
    @GetMapping(value = "/export/report/{reportType}/csv", produces = "text/csv")
    public ResponseEntity<String> exportReportAsCSV(
            @PathVariable String reportType,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> report;
            
            switch (reportType) {
                case "sla":
                    report = reportingService.generateSLAReport(days);
                    break;
                case "performance":
                    report = reportingService.generatePerformanceSummary(days);
                    break;
                case "cache":
                    report = reportingService.generateCacheReport(days);
                    break;
                default:
                    return ResponseEntity.badRequest().body("Error: Unknown report type");
            }
            
            String csv = dataExportService.exportReportAsCSV(report);
            String filename = dataExportService.getExportFilename(reportType + "-report", "csv");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);
        } catch (Exception e) {
            log.error("Error exporting report as CSV: {}", reportType, e);
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }
}
