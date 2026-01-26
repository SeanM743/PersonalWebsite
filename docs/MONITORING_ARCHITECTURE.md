# Service Health Dashboard - Architecture Documentation

## Overview

The Service Health Dashboard is a comprehensive monitoring system built using open-source tools (Prometheus, Grafana, AlertManager) integrated with a Spring Boot backend and React frontend. It provides real-time monitoring of service health, throttling metrics, cache performance, and response times.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   React UI   │  │  WebSocket   │  │   Charts &   │           │
│  │  Components  │  │    Client    │  │ Dashboards   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Monitoring   │  │ Prometheus   │  │  Reporting   │           │
│  │     API      │  │     API      │  │     API      │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Service Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   Metrics    │  │    Health    │  │    Cache     │           │
│  │   Service    │  │   Service    │  │   Metrics    │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Performance  │  │  Historical  │  │  Reporting   │           │
│  │   Metrics    │  │     Data     │  │   Service    │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
│  ┌──────────────┐  ┌──────────────┐                             │
│  │  Broadcast   │  │    Export    │                             │
│  │   Service    │  │   Service    │                             │
│  └──────────────┘  └──────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Monitoring Stack Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Prometheus  │  │   Grafana    │  │ AlertManager │          │
│  │   (Metrics)  │  │(Visualization)│  │(Alerting)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Collection Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Micrometer  │  │   Actuator   │  │   Custom     │          │
│  │   Registry   │  │  Endpoints   │  │ Interceptors │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Backend Services

#### 1.1 Metrics Collection
- **MetricsService**: Core metrics collection for throttling
- **CacheMetricsService**: Cache hit/miss tracking with time windows
- **PerformanceMetricsService**: HTTP request, database, and external API metrics
- **ServiceHealthService**: Automated health checks for all services

#### 1.2 Data Management
- **PrometheusQueryService**: Query Prometheus for historical data
- **HistoricalDataService**: Aggregate and cache historical metrics
- **ReportingService**: Generate SLA, performance, and cache reports
- **DataExportService**: Export data in CSV/JSON formats

#### 1.3 Real-time Updates
- **MetricsBroadcastService**: WebSocket broadcasts every 5-15 seconds
- **MonitoringWebSocketController**: Handle client subscriptions

#### 1.4 API Controllers
- **MonitoringController**: Health and metrics endpoints
- **PrometheusController**: Prometheus query endpoints
- **ReportingController**: Report generation and export endpoints
- **MonitoringWebSocketController**: WebSocket subscriptions

### 2. Monitoring Stack

#### 2.1 Prometheus
- **Purpose**: Time-series metrics storage and querying
- **Configuration**: `monitoring/prometheus/prometheus.yml`
- **Scraping**: Every 15 seconds from `/actuator/prometheus`
- **Retention**: 30 days, 10GB max
- **Alert Rules**: `monitoring/prometheus/alert_rules.yml`

#### 2.2 Grafana
- **Purpose**: Metrics visualization
- **Configuration**: `monitoring/grafana/grafana.ini`
- **Dashboards**:
  - Service Health Overview
  - Performance Metrics
  - Throttling & Cache Metrics
  - Alerts Overview
- **Data Source**: Prometheus

#### 2.3 AlertManager
- **Purpose**: Alert routing and notifications
- **Configuration**: `monitoring/alertmanager/alertmanager.yml`
- **Notifications**: Email, Webhook (Slack, Discord)
- **Escalation**: Multi-level policies

### 3. Metrics Collected

#### 3.1 Health Metrics
- `service_health_score` - Overall health (0-100)
- `service_health_checks_total` - Health check counts by service
- `service_health_response_time_ms` - Health check response times
- `health_check_failures_total` - Failed health checks

#### 3.2 Throttling Metrics
- `throttling_rate` - Current throttling percentage
- `requests_throttled_total` - Total throttled requests
- `throttling_decision_duration` - Time to make throttling decisions

#### 3.3 Cache Metrics
- `cache_hit_ratio` - Hit ratio by cache type
- `cache_hits_total` - Total cache hits
- `cache_misses_total` - Total cache misses
- `cache_evictions_total` - Cache evictions by reason
- `cache_operation_duration_seconds` - Cache operation timing
- `cache_size` - Current cache size

#### 3.4 Performance Metrics
- `http_request_duration_seconds` - Request duration histogram
- `http_requests_total` - Total HTTP requests
- `http_errors_total` - Total HTTP errors
- `endpoint_requests_total` - Requests per endpoint
- `endpoint_errors_total` - Errors per endpoint
- `endpoint_error_rate` - Error rate per endpoint
- `database_operation_duration_seconds` - Database operation timing
- `external_api_duration_seconds` - External API call timing

#### 3.5 JVM Metrics
- `jvm_memory_used_bytes` - Used memory
- `jvm_memory_total_bytes` - Total memory
- `jvm_memory_max_bytes` - Max memory
- `jvm_memory_utilization_percent` - Memory utilization

## Data Flow

### 1. Metrics Collection Flow
```
Application → Micrometer → Prometheus Registry → /actuator/prometheus
                                                         ↓
                                                   Prometheus
                                                         ↓
                                                    Grafana
```

### 2. Real-time Updates Flow
```
Scheduled Task → MetricsBroadcastService → WebSocket → Frontend
     (5-15s)           (STOMP)              (SockJS)
```

### 3. Historical Data Flow
```
Frontend → API → PrometheusQueryService → Prometheus → Cache → Response
                                                          ↓
                                              HistoricalDataService
```

### 4. Alerting Flow
```
Prometheus → Alert Rules → AlertManager → Notifications
                                              ↓
                                    Email / Webhook / Slack
```

## Security

### Authentication
- JWT-based authentication for all monitoring endpoints
- WebSocket connections authenticated at connection level
- Role-based access control (RBAC)

### Authorization
- **Monitoring Access**: All authenticated users
- **Admin Access**: ROLE_ADMIN only
  - Trigger health checks
  - Reset metrics
  - Access Prometheus queries

### Audit Logging
- All access attempts logged
- Security events tracked
- Failed authentication attempts recorded

## API Endpoints

### Monitoring APIs
```
GET  /api/monitoring/health                    - Overall health status
GET  /api/monitoring/health/services           - All services health
GET  /api/monitoring/health/services/{name}    - Specific service health
POST /api/monitoring/health/check              - Trigger health check (admin)
GET  /api/monitoring/metrics/throttling        - Throttling metrics
GET  /api/monitoring/metrics/cache             - Cache metrics
GET  /api/monitoring/metrics/cache/{name}      - Specific cache metrics
GET  /api/monitoring/metrics/summary           - Metrics summary
POST /api/monitoring/metrics/throttling/reset  - Reset throttling (admin)
POST /api/monitoring/metrics/cache/{name}/reset - Reset cache (admin)
POST /api/monitoring/webhook/health            - Health webhook
```

### Prometheus APIs
```
GET /api/prometheus/query                      - Instant query
GET /api/prometheus/query_range                - Range query
GET /api/prometheus/metrics/health-score       - Health score time series
GET /api/prometheus/metrics/throttling-rate    - Throttling rate time series
GET /api/prometheus/metrics/cache-hit-ratio    - Cache hit ratio time series
GET /api/prometheus/metrics/response-time      - Response time time series
GET /api/prometheus/metrics/error-rate         - Error rate time series
GET /api/prometheus/metrics/request-rate       - Request rate time series
GET /api/prometheus/dashboard                  - Dashboard data
```

### Reporting APIs
```
GET  /api/reports/sla                          - SLA report
GET  /api/reports/performance                  - Performance report
GET  /api/reports/cache                        - Cache report
GET  /api/reports/summary/daily                - Daily summary
GET  /api/reports/summary/weekly               - Weekly summary
GET  /api/reports/summary/monthly              - Monthly summary
GET  /api/reports/historical/{metric}          - Historical data
GET  /api/reports/retention/config             - Retention config
GET  /api/reports/export/{metric}/csv          - Export as CSV
GET  /api/reports/export/{metric}/json         - Export as JSON
POST /api/reports/export/bulk/csv              - Bulk export CSV
GET  /api/reports/export/report/{type}/csv     - Export report CSV
GET  /api/reports/export/report/{type}/json    - Export report JSON
```

### WebSocket Topics
```
/topic/health           - Health metrics updates (10s)
/topic/throttling       - Throttling metrics updates (5s)
/topic/cache            - Cache metrics updates (15s)
/topic/metrics-summary  - Metrics summary updates (10s)
/topic/alerts           - Real-time alerts
```

## Configuration

### Application Properties
```properties
# Monitoring
monitoring.enabled=true
monitoring.interval=15s
monitoring.prometheus.url=http://localhost:9090

# Cache Monitoring
monitoring.cache.enabled=true
monitoring.cache.update-interval=30s

# Throttling
monitoring.throttling.enabled=true
monitoring.throttling.window-size=5m
monitoring.throttling.warning-threshold=0.05
monitoring.throttling.critical-threshold=0.15

# Health Checks
monitoring.health-check.enabled=true
monitoring.health-check.interval=30s
monitoring.health-check.timeout=10s
monitoring.health-check.retry-attempts=3

# Alert Thresholds
monitoring.alerts.response-time.p95-warning=2s
monitoring.alerts.response-time.p95-critical=5s
monitoring.alerts.error-rate.warning-threshold=0.01
monitoring.alerts.error-rate.critical-threshold=0.05
```

### Prometheus Configuration
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'digital-command-center'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'

storage:
  tsdb:
    retention.time: 30d
    retention.size: 10GB
```

## Performance Considerations

### Caching Strategy
- Prometheus query results cached for 5 minutes
- Historical data cached for 5 minutes
- In-memory cache with automatic cleanup

### Optimization
- Metrics collection overhead < 1ms per request
- WebSocket broadcasts batched
- Database queries optimized with indexes
- Prometheus queries use efficient time ranges

### Scalability
- Horizontal scaling supported
- Stateless API design
- Distributed caching ready (Redis)
- Load balancer compatible

## Monitoring Best Practices

1. **Alert Fatigue Prevention**
   - Use appropriate thresholds
   - Implement alert escalation
   - Group related alerts

2. **Data Retention**
   - Short-term: 7 days (high resolution)
   - Medium-term: 30 days (medium resolution)
   - Long-term: 90 days (low resolution)

3. **Dashboard Design**
   - Focus on actionable metrics
   - Use traffic light indicators
   - Provide drill-down capabilities

4. **Performance**
   - Monitor the monitoring system
   - Set resource limits
   - Regular cleanup of old data

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**
   - Check Prometheus scraping: `http://localhost:9090/targets`
   - Verify actuator endpoint: `http://localhost:8080/actuator/prometheus`
   - Check application logs for errors

2. **High Memory Usage**
   - Adjust Prometheus retention settings
   - Clear historical data cache
   - Reduce scraping frequency

3. **WebSocket Connection Issues**
   - Check CORS configuration
   - Verify WebSocket endpoint accessibility
   - Review browser console for errors

4. **Alert Not Firing**
   - Verify alert rules syntax
   - Check AlertManager configuration
   - Review Prometheus alert status

## Future Enhancements

1. **Distributed Tracing**
   - Integration with Jaeger/Zipkin
   - Request correlation IDs
   - Span tracking

2. **Advanced Analytics**
   - Machine learning for anomaly detection
   - Predictive alerting
   - Capacity planning

3. **Multi-tenancy**
   - Per-tenant metrics
   - Isolated dashboards
   - Custom alert rules

4. **Mobile Support**
   - Mobile-responsive dashboards
   - Push notifications
   - Mobile app

## References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Micrometer Documentation](https://micrometer.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
