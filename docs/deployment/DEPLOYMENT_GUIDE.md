# Service Health Dashboard - Deployment Guide

## Quick Start - Local Testing

### Prerequisites
- Docker and Docker Compose installed
- Java 21+ (for backend)
- Node.js 18+ (for frontend)
- PostgreSQL 12+ (or use Docker)

### Step 1: Start Database
```bash
# Using Docker Compose (recommended)
docker-compose up -d postgres

# Or standalone Docker
docker run --name personal-dashboard-db \
  -e POSTGRES_DB=personal_platform \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 -d postgres:15
```

### Step 2: Start Monitoring Stack
```bash
# Start Prometheus, Grafana, and AlertManager
docker-compose -f docker-compose.monitoring.yml up -d

# Verify services are running
docker-compose -f docker-compose.monitoring.yml ps
```

**Access Monitoring Services:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)
- AlertManager: http://localhost:9093
- Node Exporter: http://localhost:9100

### Step 3: Start Backend Application
```bash
cd backend

# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or if Maven is installed globally
mvn spring-boot:run
```

**Backend will be available at:** http://localhost:8080

**Verify backend is running:**
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
- API: http://localhost:8080/api/monitoring/health

### Step 4: Start Frontend Application
```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

**Frontend will be available at:** http://localhost:5173

### Step 5: Verify Monitoring Integration

1. **Check Prometheus Targets**
   - Go to http://localhost:9090/targets
   - Verify "digital-command-center" target is UP
   - Should show endpoint: http://app:8080/actuator/prometheus

2. **Access Grafana Dashboards**
   - Go to http://localhost:3001
   - Login: admin/admin
   - Navigate to Dashboards → Browse
   - Open "Service Health Overview"

3. **Test Real-time Updates**
   - Open browser console
   - Connect to WebSocket: ws://localhost:8080/ws
   - Subscribe to topics: /topic/health, /topic/metrics-summary

4. **Generate Test Metrics**
   ```bash
   # Make some API calls to generate metrics
   curl http://localhost:8080/api/monitoring/health
   curl http://localhost:8080/api/monitoring/metrics/summary
   curl http://localhost:8080/api/portfolio
   ```

5. **View Metrics in Grafana**
   - Refresh Grafana dashboards
   - Metrics should appear within 15-30 seconds

## Production Deployment

### Docker Compose Production Setup

1. **Create production environment file**
```bash
cp .env.example .env
# Edit .env with production values
```

2. **Build production images**
```bash
# Build backend
cd backend
./mvnw clean package -DskipTests
docker build -t personal-dashboard-backend:latest .

# Build frontend
cd ../frontend
npm run build
docker build -t personal-dashboard-frontend:latest .
```

3. **Deploy with Docker Compose**
```bash
# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Start with monitoring
docker-compose -f docker-compose.prod.yml -f docker-compose.monitoring.yml up -d
```

### Kubernetes Deployment

See `ENTERPRISE_DEPLOYMENT_PATTERNS.md` for Kubernetes manifests and Helm charts.

## Configuration

### Backend Configuration

**application.properties** key settings:
```properties
# Monitoring
monitoring.enabled=true
monitoring.interval=15s
monitoring.prometheus.url=http://prometheus:9090

# Health Checks
monitoring.health-check.enabled=true
monitoring.health-check.interval=30s
monitoring.health-check.timeout=10s

# Alert Thresholds
monitoring.alerts.response-time.p95-warning=2s
monitoring.alerts.response-time.p95-critical=5s
monitoring.alerts.error-rate.warning-threshold=0.01
monitoring.alerts.error-rate.critical-threshold=0.05
```

### Prometheus Configuration

**prometheus.yml** key settings:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'digital-command-center'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Configuration

**grafana.ini** key settings:
```ini
[server]
http_port = 3000

[security]
admin_user = admin
admin_password = admin

[auth]
disable_login_form = false
```

## Monitoring Stack Management

### Start/Stop Services

```bash
# Start monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d

# Stop monitoring stack
docker-compose -f docker-compose.monitoring.yml down

# Restart specific service
docker-compose -f docker-compose.monitoring.yml restart prometheus

# View logs
docker-compose -f docker-compose.monitoring.yml logs -f prometheus
docker-compose -f docker-compose.monitoring.yml logs -f grafana
```

### Data Persistence

Monitoring data is persisted in Docker volumes:
- `prometheus_data` - Metrics data (30 days retention)
- `grafana_data` - Dashboards and settings
- `alertmanager_data` - Alert state and silences

```bash
# Backup volumes
docker run --rm -v prometheus_data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz /data

# Restore volumes
docker run --rm -v prometheus_data:/data -v $(pwd):/backup alpine tar xzf /backup/prometheus-backup.tar.gz -C /
```

### Scaling Considerations

**Horizontal Scaling:**
- Backend application is stateless and can be scaled horizontally
- Use load balancer (nginx, HAProxy) for multiple backend instances
- Prometheus federation for multiple Prometheus instances

**Vertical Scaling:**
- Increase Prometheus retention: `--storage.tsdb.retention.time=90d`
- Increase memory limits in docker-compose.yml
- Adjust scraping intervals for less frequent updates

## Security

### Authentication

**Backend API:**
- All monitoring endpoints require JWT authentication
- Admin endpoints require ROLE_ADMIN
- WebSocket connections authenticated at connection level

**Grafana:**
- Default: admin/admin (change in production!)
- Configure LDAP/OAuth for enterprise authentication
- Set `GF_SECURITY_ADMIN_PASSWORD` environment variable

**Prometheus/AlertManager:**
- No built-in authentication (use reverse proxy)
- Configure nginx with basic auth
- Use network isolation in production

### Network Security

```yaml
# docker-compose.monitoring.yml
networks:
  monitoring:
    driver: bridge
    internal: true  # Isolate monitoring network
```

### SSL/TLS

For production, configure SSL certificates:
```bash
# Generate self-signed certificate (development)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout monitoring.key -out monitoring.crt

# Use Let's Encrypt (production)
certbot certonly --standalone -d monitoring.yourdomain.com
```

## Troubleshooting

### Backend Not Connecting to Prometheus

**Symptom:** Metrics not appearing in Grafana

**Solutions:**
1. Check Prometheus targets: http://localhost:9090/targets
2. Verify backend actuator endpoint: http://localhost:8080/actuator/prometheus
3. Check Docker network connectivity:
   ```bash
   docker network inspect monitoring
   docker exec -it prometheus ping app
   ```
4. Review backend logs:
   ```bash
   cd backend && ./mvnw spring-boot:run
   # Look for "Exposing 2 endpoint(s) beneath base path '/actuator'"
   ```

### Grafana Dashboards Not Loading

**Symptom:** Dashboards show "No data"

**Solutions:**
1. Verify Prometheus data source in Grafana:
   - Configuration → Data Sources → Prometheus
   - URL should be: http://prometheus:9090
   - Click "Save & Test"

2. Check Prometheus has data:
   - Go to http://localhost:9090
   - Execute query: `up{job="digital-command-center"}`
   - Should return value: 1

3. Verify dashboard provisioning:
   ```bash
   docker exec -it grafana ls /etc/grafana/provisioning/dashboards
   # Should show: dashboard.yml and *.json files
   ```

### High Memory Usage

**Symptom:** Prometheus using too much memory

**Solutions:**
1. Reduce retention period:
   ```yaml
   # docker-compose.monitoring.yml
   command:
     - '--storage.tsdb.retention.time=15d'  # Reduce from 30d
   ```

2. Reduce scraping frequency:
   ```yaml
   # prometheus.yml
   global:
     scrape_interval: 30s  # Increase from 15s
   ```

3. Limit metric cardinality:
   ```properties
   # application.properties
   management.metrics.enable.jvm=false
   management.metrics.enable.process=false
   ```

### WebSocket Connection Failures

**Symptom:** Real-time updates not working

**Solutions:**
1. Check CORS configuration:
   ```properties
   # application.properties
   security.cors.allowed-origins=http://localhost:5173
   ```

2. Verify WebSocket endpoint:
   ```bash
   curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     http://localhost:8080/ws
   ```

3. Check browser console for errors
4. Verify authentication token is valid

### Alerts Not Firing

**Symptom:** No alerts in AlertManager

**Solutions:**
1. Check alert rules syntax:
   ```bash
   docker exec -it prometheus promtool check rules /etc/prometheus/alert_rules.yml
   ```

2. Verify AlertManager configuration:
   ```bash
   docker exec -it alertmanager amtool check-config /etc/alertmanager/alertmanager.yml
   ```

3. Check Prometheus alerts page: http://localhost:9090/alerts
4. Review AlertManager logs:
   ```bash
   docker-compose -f docker-compose.monitoring.yml logs alertmanager
   ```

## Performance Optimization

### Backend Optimization

1. **Enable caching:**
   ```properties
   monitoring.cache.enabled=true
   monitoring.cache.update-interval=30s
   ```

2. **Adjust health check frequency:**
   ```properties
   monitoring.health-check.interval=60s  # Reduce from 30s
   ```

3. **Optimize database queries:**
   - Add indexes for frequently queried columns
   - Use connection pooling
   - Enable query caching

### Prometheus Optimization

1. **Reduce metric cardinality:**
   ```yaml
   # prometheus.yml
   metric_relabel_configs:
     - source_labels: [__name__]
       regex: 'jvm_.*'
       action: drop
   ```

2. **Enable compression:**
   ```yaml
   # prometheus.yml
   storage:
     tsdb:
       wal-compression: true
   ```

3. **Optimize query performance:**
   - Use recording rules for complex queries
   - Limit time range in queries
   - Use appropriate step intervals

### Grafana Optimization

1. **Reduce dashboard refresh rate:**
   - Set refresh to 30s or 1m instead of 5s
   - Use "On Dashboard Load" for static panels

2. **Optimize queries:**
   - Use rate() instead of increase() when possible
   - Limit time range to necessary period
   - Use template variables for filtering

3. **Enable caching:**
   ```ini
   # grafana.ini
   [caching]
   enabled = true
   ```

## Maintenance

### Regular Tasks

**Daily:**
- Monitor disk space usage
- Check for failed health checks
- Review active alerts

**Weekly:**
- Review dashboard performance
- Check for metric anomalies
- Update alert thresholds if needed

**Monthly:**
- Backup Prometheus and Grafana data
- Review and archive old metrics
- Update monitoring stack versions
- Review and optimize alert rules

### Backup Procedures

```bash
# Backup script
#!/bin/bash
BACKUP_DIR="/backup/monitoring/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# Backup Prometheus data
docker run --rm -v prometheus_data:/data -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/prometheus.tar.gz /data

# Backup Grafana data
docker run --rm -v grafana_data:/data -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/grafana.tar.gz /data

# Backup configurations
cp -r monitoring/ $BACKUP_DIR/config/
```

### Update Procedures

```bash
# Update monitoring stack
docker-compose -f docker-compose.monitoring.yml pull
docker-compose -f docker-compose.monitoring.yml up -d

# Update backend
cd backend
./mvnw clean package
docker-compose restart app

# Update frontend
cd frontend
npm run build
docker-compose restart frontend
```

## Monitoring Best Practices

1. **Set appropriate alert thresholds**
   - Start conservative, adjust based on actual behavior
   - Use percentiles (p95, p99) for response times
   - Consider time windows for rate-based alerts

2. **Organize dashboards effectively**
   - Overview dashboard for high-level status
   - Detailed dashboards for specific components
   - Use consistent color schemes and layouts

3. **Document alert responses**
   - Create runbooks for common alerts
   - Document escalation procedures
   - Track alert resolution times

4. **Regular review and optimization**
   - Review alert frequency and accuracy
   - Optimize slow-performing queries
   - Remove unused metrics and dashboards

## Support and Resources

- **Prometheus Documentation:** https://prometheus.io/docs/
- **Grafana Documentation:** https://grafana.com/docs/
- **Spring Boot Actuator:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer:** https://micrometer.io/docs/

## Next Steps

1. Configure email notifications in AlertManager
2. Set up webhook integrations (Slack, Discord)
3. Implement React frontend dashboard
4. Add custom metrics for business logic
5. Create additional Grafana dashboards
6. Set up distributed tracing (Jaeger/Zipkin)
