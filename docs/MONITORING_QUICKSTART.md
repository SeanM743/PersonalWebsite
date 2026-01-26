# Service Health Dashboard - Quick Start Guide

## 🚀 Get Started in 5 Minutes!

### Step 1: Install Frontend Dependencies (1 minute)

```bash
cd frontend

# Linux/Mac
chmod +x install-monitoring-deps.sh
./install-monitoring-deps.sh

# Windows
install-monitoring-deps.bat
```

This installs:
- `sockjs-client` - WebSocket client
- `@stomp/stompjs` - STOMP messaging protocol

### Step 2: Start All Services (2 minutes)

Open 4 terminals:

**Terminal 1 - Database:**
```bash
docker-compose up -d postgres
```

**Terminal 2 - Monitoring Stack:**
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

Wait for all services to be healthy (check with `docker ps`).

**Terminal 3 - Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

Wait for: `Started BackendApplication in X seconds`

**Terminal 4 - Frontend:**
```bash
cd frontend
npm run dev
```

Wait for: `Local: http://localhost:5173/`

### Step 3: Access the Dashboard (30 seconds)

Open your browser and navigate to:

**React Monitoring Dashboard:**
- URL: http://localhost:5173/monitoring
- Login: admin / password
- Click "Monitoring" in the navigation bar

**Alternative - Grafana:**
- URL: http://localhost:3001
- Login: admin / admin
- Navigate to Dashboards → Browse

### Step 4: Verify Everything Works (1 minute)

1. **Check Service Health:**
   - You should see service health cards with green/yellow/red indicators
   - Health scores should be displayed (0-100)

2. **View Performance Metrics:**
   - Click the "Performance" tab
   - You should see response time, error rate, and request rate charts

3. **Check Real-time Updates:**
   - Watch the metrics update automatically every few seconds
   - The timestamp at the bottom should update

4. **Generate Test Data:**
   ```bash
   # In a new terminal
   for i in {1..50}; do
     curl http://localhost:8080/api/monitoring/health
     curl http://localhost:8080/api/monitoring/metrics/summary
     sleep 0.1
   done
   ```

5. **Watch Metrics Update:**
   - Go back to the monitoring dashboard
   - Metrics should update within 15-30 seconds
   - Charts should show the new data points

## ✅ You're Done!

Your monitoring system is now fully operational!

## 🎯 What You Can Do Now

### Explore the Dashboard

**Overview Tab:**
- View all service health statuses
- See traffic light indicators (green/yellow/red)
- Check health scores and response times
- Trigger manual health checks

**Performance Tab:**
- View response time trends
- Monitor error rates
- Track request rates
- Change time ranges (1h, 6h, 24h, 7d)

**Throttling Tab:**
- Monitor throttling rates
- View throttling by endpoint
- Check throttling thresholds
- Reset throttling metrics

**Cache Tab:**
- View cache hit ratios
- Monitor cache performance by type
- Check cache utilization
- Reset cache metrics

**Alerts Tab:**
- View active alerts
- Filter by status and severity
- See alert history
- Link to AlertManager

### Access Other Interfaces

**Prometheus:**
- URL: http://localhost:9090
- Query metrics directly
- View targets and alerts
- Explore time-series data

**Grafana:**
- URL: http://localhost:3001
- View pre-built dashboards
- Create custom dashboards
- Set up alerts

**AlertManager:**
- URL: http://localhost:9093
- Manage alerts
- Configure notifications
- View alert history

## 🔧 Common Tasks

### Generate More Test Data

```bash
# High load test
ab -n 1000 -c 10 http://localhost:8080/api/monitoring/health

# Continuous monitoring
watch -n 1 'curl -s http://localhost:8080/api/monitoring/metrics/summary | jq'
```

### Check WebSocket Connection

Open browser DevTools (F12) → Console:
```javascript
// You should see WebSocket connection messages
// Look for: "STOMP: Connected"
```

### View Backend Metrics

```bash
# Health endpoint
curl http://localhost:8080/actuator/health | jq

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Monitoring API
curl http://localhost:8080/api/monitoring/metrics/summary | jq
```

### Check Prometheus Targets

1. Go to: http://localhost:9090/targets
2. Verify "digital-command-center" is UP
3. Check last scrape time (should be recent)

### View Grafana Dashboards

1. Go to: http://localhost:3001
2. Login: admin / admin
3. Navigate: Dashboards → Browse
4. Open any dashboard:
   - Service Health Overview
   - Performance Metrics
   - Throttling & Cache Metrics
   - Alerts Overview

## 🐛 Troubleshooting

### Frontend Won't Start

**Error:** "Cannot find module 'sockjs-client'"
```bash
cd frontend
npm install sockjs-client @stomp/stompjs
```

### No Metrics Displayed

**Check 1:** Backend running?
```bash
curl http://localhost:8080/actuator/health
```

**Check 2:** Prometheus scraping?
- Go to: http://localhost:9090/targets
- Verify target is UP

**Check 3:** Generate test data
```bash
for i in {1..10}; do curl http://localhost:8080/api/monitoring/health; done
```

### WebSocket Not Connecting

**Check 1:** Backend WebSocket endpoint
```bash
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" http://localhost:8080/ws
```

**Check 2:** Browser console
- Open DevTools (F12)
- Look for WebSocket errors
- Check authentication token

**Check 3:** CORS configuration
- Verify `security.cors.allowed-origins` includes `http://localhost:5173`

### Monitoring Stack Not Starting

**Check Docker:**
```bash
docker-compose -f docker-compose.monitoring.yml ps
docker-compose -f docker-compose.monitoring.yml logs
```

**Restart Services:**
```bash
docker-compose -f docker-compose.monitoring.yml down
docker-compose -f docker-compose.monitoring.yml up -d
```

## 📚 Next Steps

1. **Configure Alerts:**
   - Edit `monitoring/alertmanager/alertmanager.yml`
   - Add email/Slack/Discord notifications
   - Test alert delivery

2. **Customize Dashboards:**
   - Create custom Grafana dashboards
   - Add business-specific metrics
   - Set up custom alerts

3. **Explore Documentation:**
   - `MONITORING_ARCHITECTURE.md` - Architecture details
   - `DEPLOYMENT_GUIDE.md` - Production deployment
   - `SETUP.md` - Detailed setup guide
   - `frontend/MONITORING_SETUP.md` - Frontend details

4. **Production Deployment:**
   - Review security settings
   - Configure SSL/TLS
   - Set up backup procedures
   - Implement monitoring for monitoring

## 🎉 Success!

You now have a fully functional monitoring system with:
- ✅ Real-time metrics collection
- ✅ Beautiful React dashboard
- ✅ Grafana visualizations
- ✅ Automated alerting
- ✅ WebSocket live updates
- ✅ Historical data analysis
- ✅ Data export capabilities

Start monitoring your application and enjoy the insights! 📊

## 💡 Tips

- **Bookmark the dashboard:** http://localhost:5173/monitoring
- **Check metrics regularly** to establish baselines
- **Set up alerts** for critical thresholds
- **Review Grafana dashboards** for detailed analysis
- **Export data** for long-term analysis
- **Monitor the monitoring system** itself

## 📞 Need Help?

1. Check browser console for errors
2. Review backend logs
3. Verify all services are running
4. Check `DEPLOYMENT_GUIDE.md` troubleshooting section
5. Review `MONITORING_ARCHITECTURE.md` for API details

---

**Happy Monitoring!** 🚀
