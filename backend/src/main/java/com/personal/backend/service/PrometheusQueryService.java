package com.personal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusQueryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${monitoring.prometheus.url:http://localhost:9090}")
    private String prometheusUrl;

    /**
     * Execute instant query against Prometheus
     */
    @Cacheable(value = "prometheus-queries", key = "#query + '-' + #time")
    public Map<String, Object> queryInstant(String query, Long time) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", query)
                    .queryParam("time", time != null ? time : Instant.now().getEpochSecond())
                    .toUriString();

            log.debug("Executing Prometheus instant query: {}", query);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            return parsePrometheusResponse(jsonResponse);
        } catch (Exception e) {
            log.error("Error executing Prometheus instant query: {}", query, e);
            return Map.of("error", "Failed to execute query", "message", e.getMessage());
        }
    }

    /**
     * Execute range query against Prometheus
     */
    @Cacheable(value = "prometheus-range-queries", key = "#query + '-' + #start + '-' + #end + '-' + #step")
    public Map<String, Object> queryRange(String query, Long start, Long end, String step) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start != null ? start : Instant.now().minusSeconds(3600).getEpochSecond())
                    .queryParam("end", end != null ? end : Instant.now().getEpochSecond())
                    .queryParam("step", step != null ? step : "15s")
                    .toUriString();

            log.debug("Executing Prometheus range query: {}", query);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            return parsePrometheusResponse(jsonResponse);
        } catch (Exception e) {
            log.error("Error executing Prometheus range query: {}", query, e);
            return Map.of("error", "Failed to execute query", "message", e.getMessage());
        }
    }

    /**
     * Get current value of a metric
     */
    public Double getMetricValue(String metricName) {
        try {
            Map<String, Object> result = queryInstant(metricName, null);
            
            if (result.containsKey("error")) {
                return null;
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("result");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> firstResult = data.get(0);
                List<Object> value = (List<Object>) firstResult.get("value");
                if (value != null && value.size() > 1) {
                    return Double.parseDouble(value.get(1).toString());
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error getting metric value for: {}", metricName, e);
            return null;
        }
    }

    /**
     * Get metric values over time range
     */
    public List<Map<String, Object>> getMetricTimeSeries(String metricName, Long start, Long end, String step) {
        try {
            Map<String, Object> result = queryRange(metricName, start, end, step);
            
            if (result.containsKey("error")) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("result");
            if (data == null || data.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> timeSeries = new ArrayList<>();
            for (Map<String, Object> series : data) {
                List<List<Object>> values = (List<List<Object>>) series.get("values");
                Map<String, Object> metric = (Map<String, Object>) series.get("metric");
                
                for (List<Object> value : values) {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", ((Number) value.get(0)).longValue());
                    point.put("value", Double.parseDouble(value.get(1).toString()));
                    point.put("metric", metric);
                    timeSeries.add(point);
                }
            }
            
            return timeSeries;
        } catch (Exception e) {
            log.error("Error getting metric time series for: {}", metricName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get health score metric
     */
    public Double getHealthScore() {
        return getMetricValue("service_health_score");
    }

    /**
     * Get throttling rate metric
     */
    public Double getThrottlingRate() {
        return getMetricValue("throttling_rate");
    }

    /**
     * Get cache hit ratio for specific cache
     */
    public Double getCacheHitRatio(String cacheName) {
        String query = String.format("cache_hit_ratio{cache_name=\"%s\"}", cacheName);
        return getMetricValue(query);
    }

    /**
     * Get response time percentile
     */
    public Double getResponseTimePercentile(double percentile, String endpoint) {
        String query = endpoint != null
                ? String.format("histogram_quantile(%.2f, rate(http_request_duration_seconds_bucket{endpoint=\"%s\"}[5m]))", 
                        percentile, endpoint)
                : String.format("histogram_quantile(%.2f, rate(http_request_duration_seconds_bucket[5m]))", percentile);
        return getMetricValue(query);
    }

    /**
     * Get error rate
     */
    public Double getErrorRate(String endpoint) {
        String query = endpoint != null
                ? String.format("rate(http_errors_total{endpoint=\"%s\"}[5m])", endpoint)
                : "rate(http_errors_total[5m])";
        return getMetricValue(query);
    }

    /**
     * Get request rate
     */
    public Double getRequestRate(String endpoint) {
        String query = endpoint != null
                ? String.format("rate(http_requests_total{endpoint=\"%s\"}[5m])", endpoint)
                : "rate(http_requests_total[5m])";
        return getMetricValue(query);
    }

    /**
     * Build complex query with filters
     */
    public PrometheusQueryBuilder queryBuilder() {
        return new PrometheusQueryBuilder(this);
    }

    /**
     * Parse Prometheus API response
     */
    private Map<String, Object> parsePrometheusResponse(JsonNode response) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            String status = response.get("status").asText();
            result.put("status", status);
            
            if ("success".equals(status)) {
                JsonNode data = response.get("data");
                String resultType = data.get("resultType").asText();
                result.put("resultType", resultType);
                
                JsonNode resultNode = data.get("result");
                List<Map<String, Object>> resultList = new ArrayList<>();
                
                for (JsonNode item : resultNode) {
                    Map<String, Object> resultItem = new HashMap<>();
                    
                    // Parse metric labels
                    JsonNode metric = item.get("metric");
                    if (metric != null) {
                        Map<String, Object> metricMap = new HashMap<>();
                        metric.fields().forEachRemaining(entry -> 
                            metricMap.put(entry.getKey(), entry.getValue().asText())
                        );
                        resultItem.put("metric", metricMap);
                    }
                    
                    // Parse value or values
                    if (item.has("value")) {
                        JsonNode value = item.get("value");
                        List<Object> valueList = new ArrayList<>();
                        valueList.add(value.get(0).asLong());
                        valueList.add(value.get(1).asText());
                        resultItem.put("value", valueList);
                    } else if (item.has("values")) {
                        JsonNode values = item.get("values");
                        List<List<Object>> valuesList = new ArrayList<>();
                        for (JsonNode value : values) {
                            List<Object> valueList = new ArrayList<>();
                            valueList.add(value.get(0).asLong());
                            valueList.add(value.get(1).asText());
                            valuesList.add(valueList);
                        }
                        resultItem.put("values", valuesList);
                    }
                    
                    resultList.add(resultItem);
                }
                
                result.put("result", resultList);
            } else {
                String errorType = response.get("errorType").asText();
                String error = response.get("error").asText();
                result.put("errorType", errorType);
                result.put("error", error);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error parsing Prometheus response", e);
            return Map.of("error", "Failed to parse response", "message", e.getMessage());
        }
    }

    /**
     * Query builder for complex Prometheus queries
     */
    public static class PrometheusQueryBuilder {
        private final PrometheusQueryService service;
        private String metric;
        private final Map<String, String> labels = new HashMap<>();
        private String function;
        private String range = "5m";

        public PrometheusQueryBuilder(PrometheusQueryService service) {
            this.service = service;
        }

        public PrometheusQueryBuilder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public PrometheusQueryBuilder label(String key, String value) {
            this.labels.put(key, value);
            return this;
        }

        public PrometheusQueryBuilder function(String function) {
            this.function = function;
            return this;
        }

        public PrometheusQueryBuilder range(String range) {
            this.range = range;
            return this;
        }

        public String build() {
            StringBuilder query = new StringBuilder();
            
            if (function != null) {
                query.append(function).append("(");
            }
            
            query.append(metric);
            
            if (!labels.isEmpty()) {
                query.append("{");
                labels.forEach((key, value) -> 
                    query.append(key).append("=\"").append(value).append("\",")
                );
                query.setLength(query.length() - 1); // Remove trailing comma
                query.append("}");
            }
            
            if (function != null && function.startsWith("rate")) {
                query.append("[").append(range).append("]");
            }
            
            if (function != null) {
                query.append(")");
            }
            
            return query.toString();
        }

        public Map<String, Object> execute() {
            return service.queryInstant(build(), null);
        }

        public Map<String, Object> executeRange(Long start, Long end, String step) {
            return service.queryRange(build(), start, end, step);
        }
    }
}
