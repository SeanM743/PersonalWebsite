# Monitoring Dashboard - Frontend Setup

## Overview

The monitoring dashboard provides a comprehensive React-based interface for viewing service health, performance metrics, throttling data, cache performance, and alerts in real-time.

## Features

- **Service Health Overview**: Real-time health status with traffic light indicators
- **Performance Metrics**: Response times, error rates, and request rates with interactive charts
- **Throttling Metrics**: Monitor request throttling across endpoints
- **Cache Performance**: Track cache hit ratios and performance by cache type
- **Alert Management**: View and manage alerts from Prometheus/AlertManager
- **Real-time Updates**: WebSocket integration for live metric streaming

## Installation

### 1. Install Dependencies

```bash
# Linux/Mac
chmod +x install-monitoring-deps.sh
./install-monitoring-deps.sh

# Windows
install-monitoring-deps.bat

# Or manually
npm install sockjs-client @stomp/stompjs
```

### 2. Start the Application

```bash
npm run dev
```

The frontend will be available at: http://localhost:5173

## Components

### Pages

- **Monitoring.tsx** - Main monitoring dashboard with tab navigation
  - Overview tab - Service health status
  - Performance tab - Metrics charts
  - Throttling tab - Throttling metrics
  - Cache tab - Cache performance
  - Alerts tab - Alert management

### Components

- **ServiceHealthOverview.tsx** - Service health cards with status indicators
- **MetricsCharts.tsx** - Performance metrics visualization
- **ThrottlingMetrics.tsx** - Throttling rate display and analysis
- **CacheMetrics.tsx** - Cache hit ratio and performance metrics
- **AlertsPanel.tsx** - Alert list and management interface

### Services

- **monitoringService.ts** - API client and WebSocket connection manager
  - REST API calls to backend monitoring endpoints
  - WebSocket subscriptions for real-time updates
  - Prometheus query integration
  - Report generation and data export

## Usage

### Accessing the Dashboard

1. Start the backend application
2. Start the monitoring stack (Prometheus, Grafana, AlertManager)
3. Start the frontend application
4. Navigate to: http://localhost:5173/monitoring

### Navigation

The monitoring dashboard is accessible from the main navigation bar:
- Click "Monitoring" in the header
- Or navigate directly to `/monitoring`

### Real-time Updates

The dashboard automatically connects to the backend WebSocket and subscribes to:
- Health updates (every 10 seconds)
- Throttling updates (every 5 seconds)
- Cache updates (every 15 seconds)
- Metrics summary (every 10 seconds)
- Alert notifications (real-time)

### Time Range Selection

Most charts support time range selection:
- Last 1 hour
- Last 6 hours
- Last 24 hours (default)
- Last 7 days

### Exporting Data

Data can be exported from the backend API:
```typescript
// Export metrics as CSV
await monitoringService.exportMetricCSV('response-time', startDate, endDate);

// Export metrics as JSON
await monitoringService.exportMetricJSON('cache-hit-ratio', startDate, endDate);

// Bulk export
await monitoringService.exportBulkCSV(['response-time', 'error-rate'], startDate, endDate);
```

## Configuration

### API Base URL

The frontend connects to the backend API at:
```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
```

To change the API URL, set the `VITE_API_BASE_URL` environment variable:
```bash
# .env file
VITE_API_BASE_URL=http://your-backend-url:8080
```

### WebSocket Connection

WebSocket connections are established automatically when visiting the monitoring page:
```typescript
// Connection URL
ws://localhost:8080/ws

// Topics
/topic/health           - Health metrics
/topic/throttling       - Throttling metrics
/topic/cache            - Cache metrics
/topic/metrics-summary  - Metrics summary
/topic/alerts           - Real-time alerts
```

## Troubleshooting

### WebSocket Connection Fails

**Problem**: Real-time updates not working

**Solutions**:
1. Check backend is running: http://localhost:8080/actuator/health
2. Verify WebSocket endpoint: http://localhost:8080/ws
3. Check browser console for connection errors
4. Verify CORS configuration in backend
5. Check authentication token is valid

### No Data Displayed

**Problem**: Charts show "No data available"

**Solutions**:
1. Verify backend is collecting metrics
2. Check Prometheus is scraping: http://localhost:9090/targets
3. Generate test data by making API calls
4. Check time range selection (may need shorter range)
5. Verify backend actuator endpoint: http://localhost:8080/actuator/prometheus

### Charts Not Rendering

**Problem**: Charts appear broken or don't render

**Solutions**:
1. Check browser console for errors
2. Verify all dependencies are installed
3. Clear browser cache and reload
4. Check data format from API responses
5. Verify SVG rendering is supported

### Authentication Errors

**Problem**: 401 Unauthorized errors

**Solutions**:
1. Login to the application first
2. Check JWT token in localStorage
3. Verify token hasn't expired
4. Check backend authentication configuration
5. Try logging out and back in

## Development

### Adding New Metrics

1. Add API method to `monitoringService.ts`:
```typescript
async getNewMetric() {
  const response = await this.api.get('/monitoring/metrics/new-metric');
  return response.data;
}
```

2. Create component to display metric:
```typescript
// components/Monitoring/NewMetricDisplay.tsx
import { monitoringService } from '../../services/monitoringService';

const NewMetricDisplay: React.FC = () => {
  const [data, setData] = useState(null);
  
  useEffect(() => {
    loadData();
  }, []);
  
  const loadData = async () => {
    const response = await monitoringService.getNewMetric();
    setData(response.data);
  };
  
  return <div>{/* Display metric */}</div>;
};
```

3. Add to Monitoring page:
```typescript
// pages/Monitoring.tsx
import NewMetricDisplay from '../components/Monitoring/NewMetricDisplay';

// Add tab
const tabs = [
  // ... existing tabs
  { id: 'new-metric', label: 'New Metric', icon: Icon },
];

// Add content
{activeTab === 'new-metric' && <NewMetricDisplay />}
```

### Adding WebSocket Subscriptions

1. Add subscription method to `monitoringService.ts`:
```typescript
subscribeToNewTopic(callback: (data: any) => void): () => void {
  if (!this.stompClient?.connected) return () => {};
  
  const subscription = this.stompClient.subscribe('/topic/new-topic', (message) => {
    const data = JSON.parse(message.body);
    callback(data);
  });
  
  return () => subscription.unsubscribe();
}
```

2. Use in component:
```typescript
useEffect(() => {
  const unsubscribe = monitoringService.subscribeToNewTopic((data) => {
    setData(data);
  });
  
  return () => unsubscribe();
}, []);
```

## Testing

### Manual Testing

1. Start all services
2. Navigate to monitoring dashboard
3. Verify all tabs load correctly
4. Check real-time updates are working
5. Test time range selection
6. Verify charts render correctly
7. Test refresh buttons
8. Check alert display

### Load Testing

Generate test data to verify dashboard performance:
```bash
# Generate 1000 requests
for i in {1..1000}; do
  curl http://localhost:8080/api/monitoring/health
  sleep 0.1
done
```

Watch metrics update in real-time on the dashboard.

## Performance

### Optimization Tips

1. **Limit data points**: Charts display last 20-30 points for mini charts
2. **Caching**: API responses are cached for 30-60 seconds
3. **Lazy loading**: Components load data only when tab is active
4. **Debouncing**: Time range changes are debounced
5. **Memoization**: Use React.memo for expensive components

### Memory Management

- WebSocket connections are cleaned up on unmount
- Intervals are cleared on component unmount
- Subscriptions are unsubscribed when not needed
- Large datasets are paginated or limited

## Architecture

```
frontend/src/
├── pages/
│   └── Monitoring.tsx              # Main monitoring page
├── components/
│   └── Monitoring/
│       ├── ServiceHealthOverview.tsx
│       ├── MetricsCharts.tsx
│       ├── ThrottlingMetrics.tsx
│       ├── CacheMetrics.tsx
│       └── AlertsPanel.tsx
└── services/
    └── monitoringService.ts        # API client & WebSocket
```

## Integration with Backend

The frontend integrates with these backend endpoints:

### REST APIs
- `/api/monitoring/*` - Health and metrics endpoints
- `/api/prometheus/*` - Prometheus query endpoints
- `/api/reports/*` - Report generation and export

### WebSocket
- `/ws` - WebSocket connection endpoint
- `/topic/*` - STOMP topics for real-time updates

See `MONITORING_ARCHITECTURE.md` for complete API documentation.

## Next Steps

1. Install dependencies: `./install-monitoring-deps.sh`
2. Start backend and monitoring stack
3. Start frontend: `npm run dev`
4. Navigate to: http://localhost:5173/monitoring
5. Explore the dashboard and verify all features work

## Support

For issues or questions:
1. Check browser console for errors
2. Verify backend is running and accessible
3. Check WebSocket connection status
4. Review `DEPLOYMENT_GUIDE.md` for troubleshooting
5. Check `MONITORING_ARCHITECTURE.md` for API details

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [STOMP Protocol](https://stomp.github.io/)
- [SockJS](https://github.com/sockjs/sockjs-client)
- [React Documentation](https://react.dev/)
