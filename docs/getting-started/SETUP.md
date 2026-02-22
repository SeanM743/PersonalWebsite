# Quick Setup Guide - Service Health Dashboard

## 🚀 5-Minute Local Setup

### Step 1: Start Database (30 seconds)
```bash
docker-compose up -d postgres
```

### Step 2: Start Monitoring Stack (1 minute)
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

Wait for services to be healthy:
```bash
# Check status
docker-compose -f docker-compose.monitoring.yml ps

# All services should show "healthy" status
```

### Step 3: Start Backend (1 minute)
```bash
cd backend
./mvnw spring-boot:run
```

Wait for: `Started BackendApplication in X seconds`

### Step 4: Start Frontend (1 minute)
```bash
cd frontend
npm install  # First time only
npm run dev
```

Wait for: `Local: http://localhost:5173/`

### Step 5: Verify Everything Works (2 minutes)

**1. Check Backend Health:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

**2. Check Prometheus:**
- Open: http://localhost:9090/targets
- Verify: "digital-command-center" target is UP

**3. Check Grafana:**
- Open: http://localhost:3001
- Login: admin / admin
- Navigate: Dashboards → Browse → Service Health Overview

**4. Check Frontend:**
- Open: http://localhost:5173
- Login: admin / password
- Navigate to Dashboard

**5. Generate Test Data:**
```bash
# Make some API calls
curl http://localhost:8080/api/monitoring/health
curl http://localhost:8080/api/monitoring/metrics/summary
curl http://localhost:8080/api/portfolio
```

**6. View Metrics:**
- Refresh Grafana dashboard
- Metrics should appear within 15-30 seconds

## ✅ You're Done!

Your monitoring system is now running:
- ✅ Backend API: http://localhost:8080
- ✅ Frontend: http://localhost:5173
- ✅ Prometheus: http://localhost:9090
- ✅ Grafana: http://localhost:3001
- ✅ AlertManager: http://localhost:9093

## 🎯 What to Do Next

### Explore Grafana Dashboards

1. **Service Health Overview**
   - Overall health status with traffic lights
   - Service uptime and availability
   - Recent health check results

2. **Performance Metrics**
   - Response time charts (p50, p95, p99)
   - Endpoint performance comparison
   - Performance trends

3. **Throttling & Cache Metrics**
   - Throttling rate charts
   - Cache hit ratio visualizations
   - Performance over time

4. **Alerts Overview**
   - Active alerts
   - Alert history
   - Alert management

### Test Real-time Updates

Open browser console and connect to WebSocket:
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to health updates
    stompClient.subscribe('/topic/health', function(message) {
        console.log('Health update:', JSON.parse(message.body));
    });
    
    // Subscribe to metrics summary
    stompClient.subscribe('/topic/metrics-summary', function(message) {
        console.log('Metrics update:', JSON.parse(message.body));
    });
});
```

### Generate Load for Testing

```bash
# Install Apache Bench (if not installed)
# Ubuntu: sudo apt-get install apache2-utils
# Mac: brew install httpd

# Generate 1000 requests
ab -n 1000 -c 10 http://localhost:8080/api/monitoring/health

# Watch metrics update in Grafana
```

### Configure Alerts

Edit `monitoring/alertmanager/alertmanager.yml` to add email notifications:
```yaml
receivers:
  - name: 'email'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
```

Restart AlertManager:
```bash
docker-compose -f docker-compose.monitoring.yml restart alertmanager
```

## 🔧 Troubleshooting

### Backend Won't Start

**Error:** "Port 8080 already in use"
```bash
# Find and kill process using port 8080
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac:
lsof -ti:8080 | xargs kill -9
```

**Error:** "Could not connect to database"
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# If not running, start it
docker-compose up -d postgres

# Check logs
docker logs personal-dashboard-db
```

### Prometheus Not Scraping Metrics

**Check 1:** Verify backend actuator endpoint
```bash
curl http://localhost:8080/actuator/prometheus
# Should return metrics in Prometheus format
```

**Check 2:** Verify Prometheus configuration
```bash
docker exec -it prometheus cat /etc/prometheus/prometheus.yml
# Should have job_name: 'digital-command-center'
```

**Check 3:** Check Prometheus logs
```bash
docker logs prometheus
# Look for scraping errors
```

**Fix:** Restart Prometheus
```bash
docker-compose -f docker-compose.monitoring.yml restart prometheus
```

### Grafana Shows "No Data"

**Check 1:** Verify Prometheus data source
- Go to: http://localhost:3001/datasources
- Click on "Prometheus"
- URL should be: http://prometheus:9090
- Click "Save & Test" - should show green checkmark

**Check 2:** Verify Prometheus has data
```bash
# Open Prometheus UI
# Go to: http://localhost:9090
# Execute query: up{job="digital-command-center"}
# Should return: 1
```

**Check 3:** Check dashboard time range
- Grafana dashboards default to "Last 6 hours"
- If backend just started, change to "Last 5 minutes"

### Frontend Can't Connect to Backend

**Check 1:** Verify CORS configuration
```properties
# backend/src/main/resources/application.properties
security.cors.allowed-origins=http://localhost:5173
```

**Check 2:** Check browser console for errors
- Open DevTools (F12)
- Look for CORS or network errors

**Check 3:** Verify backend is running
```bash
curl http://localhost:8080/actuator/health
```

### WebSocket Connection Fails

**Check 1:** Verify WebSocket endpoint
```bash
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  http://localhost:8080/ws
# Should return: 101 Switching Protocols
```

**Check 2:** Check authentication
- WebSocket requires valid JWT token
- Login first, then connect with token

**Check 3:** Check browser console
- Look for WebSocket connection errors
- Verify SockJS/STOMP client is loaded

## 📊 Understanding the Metrics

### Health Score (0-100)
- **90-100**: Excellent (Green)
- **70-89**: Good (Yellow)
- **50-69**: Fair (Orange)
- **0-49**: Poor (Red)

### Throttling Rate
- **< 5%**: Normal (Green)
- **5-15%**: Warning (Yellow)
- **> 15%**: Critical (Red)

### Cache Hit Ratio
- **> 80%**: Excellent (Green)
- **60-80%**: Good (Yellow)
- **< 60%**: Poor (Red)

### Response Time (p95)
- **< 200ms**: Excellent (Green)
- **200-500ms**: Good (Yellow)
- **500-2000ms**: Warning (Orange)
- **> 2000ms**: Critical (Red)

### Error Rate
- **< 1%**: Normal (Green)
- **1-5%**: Warning (Yellow)
- **> 5%**: Critical (Red)

## 🎓 Learning Resources

### Prometheus Queries

**Basic Queries:**
```promql
# Current health score
service_health_score

# Request rate (per second)
rate(http_requests_total[5m])

# Error rate
rate(http_errors_total[5m]) / rate(http_requests_total[5m])

# p95 response time
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Cache hit ratio
cache_hits_total / (cache_hits_total + cache_misses_total)
```

**Advanced Queries:**
```promql
# Top 5 slowest endpoints
topk(5, histogram_quantile(0.95, 
  rate(http_request_duration_seconds_bucket[5m])))

# Endpoints with highest error rate
topk(5, rate(endpoint_errors_total[5m]) / 
  rate(endpoint_requests_total[5m]))

# Memory utilization trend
avg_over_time(jvm_memory_utilization_percent[1h])
```

### Grafana Tips

1. **Use Variables:**
   - Create dashboard variables for dynamic filtering
   - Example: $service, $endpoint, $time_range

2. **Set Alerts:**
   - Configure alert rules in panels
   - Set notification channels
   - Test alerts before enabling

3. **Organize Panels:**
   - Use rows to group related panels
   - Add descriptions to panels
   - Use consistent colors and units

4. **Share Dashboards:**
   - Export dashboard JSON
   - Share via snapshot
   - Create dashboard links

## 🚀 Production Deployment

For production deployment, see:
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Complete deployment guide
- **[ENTERPRISE_DEPLOYMENT_PATTERNS.md](ENTERPRISE_DEPLOYMENT_PATTERNS.md)** - Kubernetes and enterprise patterns
- **[MONITORING_ARCHITECTURE.md](MONITORING_ARCHITECTURE.md)** - Architecture details

## 📞 Need Help?

1. Check the troubleshooting section above
2. Review logs:
   ```bash
   # Backend logs
   cd backend && ./mvnw spring-boot:run
   
   # Prometheus logs
   docker logs prometheus
   
   # Grafana logs
   docker logs grafana
   ```
3. Verify all services are running:
   ```bash
   docker-compose ps
   docker-compose -f docker-compose.monitoring.yml ps
   ```

## 🎉 Success!

You now have a fully functional monitoring system with:
- ✅ Real-time metrics collection
- ✅ Beautiful Grafana dashboards
- ✅ Automated health checks
- ✅ Alert management
- ✅ Historical data analysis
- ✅ WebSocket real-time updates

Start exploring and monitoring your application! 📊
