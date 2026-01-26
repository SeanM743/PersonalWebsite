package com.personal.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataExportService {

    private final HistoricalDataService historicalDataService;
    private final ObjectMapper objectMapper;

    /**
     * Export data as CSV
     */
    public String exportAsCSV(String metricName, int days) {
        try {
            List<Map<String, Object>> data = historicalDataService.exportHistoricalData(metricName, days);
            
            if (data.isEmpty()) {
                return "No data available for export";
            }
            
            StringBuilder csv = new StringBuilder();
            
            // Header
            csv.append("Timestamp,DateTime,Value,Metric\n");
            
            // Data rows
            for (Map<String, Object> row : data) {
                csv.append(row.get("timestamp")).append(",");
                csv.append(row.get("datetime")).append(",");
                csv.append(row.get("value")).append(",");
                csv.append(row.get("metric")).append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting data as CSV", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Export data as JSON
     */
    public String exportAsJSON(String metricName, int days) {
        try {
            List<Map<String, Object>> data = historicalDataService.exportHistoricalData(metricName, days);
            
            Map<String, Object> export = Map.of(
                "metric", metricName,
                "period", days + " days",
                "exportedAt", Instant.now().toString(),
                "dataPoints", data.size(),
                "data", data
            );
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        } catch (Exception e) {
            log.error("Error exporting data as JSON", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Export multiple metrics as CSV
     */
    public String exportMultipleMetricsAsCSV(List<String> metrics, int days) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Timestamp,DateTime,Metric,Value\n");
            
            for (String metric : metrics) {
                List<Map<String, Object>> data = historicalDataService.exportHistoricalData(metric, days);
                
                for (Map<String, Object> row : data) {
                    csv.append(row.get("timestamp")).append(",");
                    csv.append(row.get("datetime")).append(",");
                    csv.append(row.get("metric")).append(",");
                    csv.append(row.get("value")).append("\n");
                }
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting multiple metrics as CSV", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Export report as JSON
     */
    public String exportReportAsJSON(Map<String, Object> report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (Exception e) {
            log.error("Error exporting report as JSON", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Export report as CSV (flattened)
     */
    public String exportReportAsCSV(Map<String, Object> report) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Section,Key,Value\n");
            
            flattenMap("", report, csv);
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting report as CSV", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get export filename
     */
    public String getExportFilename(String metricName, String format) {
        String timestamp = Instant.now().toString().replace(":", "-");
        return String.format("%s_%s.%s", metricName, timestamp, format.toLowerCase());
    }

    private void flattenMap(String prefix, Map<String, Object> map, StringBuilder csv) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, csv);
            } else if (value instanceof List) {
                csv.append(key).append(",").append("list").append(",").append(value).append("\n");
            } else {
                csv.append(key).append(",").append(entry.getKey()).append(",").append(value).append("\n");
            }
        }
    }
}
