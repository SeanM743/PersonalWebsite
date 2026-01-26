# 🚀 START HERE - Personal Dashboard with Monitoring

## Quick Start (2 Commands!)

### Linux/Mac
```bash
chmod +x start-all.sh
./start-all.sh
```

### Windows
```cmd
start-all.bat
```

**That's it!** The script handles everything automatically.

## What You Get

After running the startup script, you'll have:

### 📱 Main Application
- **URL**: http://localhost:5173
- **Login**: admin / password

**Features:**
- 🏠 Dashboard - Overview of everything
- 💰 Portfolio - Real-time stock tracking
- 📅 Calendar - Google Calendar integration
- 📝 Content - Social media-style posts
- 💬 Chat - AI-powered assistant
- 📊 **Monitoring** - Service health dashboard

### 📊 Monitoring System
- **React Dashboard**: http://localhost:5173/monitoring
- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093

**Features:**
- Real-time service health monitoring
- Performance metrics (response times, error rates)
- Throttling and cache metrics
- Alert management
- Historical data analysis
- Data export (CSV/JSON)

## Prerequisites

You need these installed:

1. **Docker & Docker Compose** - https://docs.docker.com/get-docker/
2. **Java 21+** - https://adoptium.net/
3. **Node.js 18+** - https://nodejs.org/

Check if installed:
```bash
docker --version
docker-compose --version
java -version
node --version
npm --version
```

## Startup Time

- **First time**: 3-5 minutes (downloading dependencies)
- **Subsequent times**: 1-2 minutes (cached)

## What Gets Started

The startup script automatically starts:

1. ✅ **PostgreSQL Database** (port 5432)
2. ✅ **Prometheus** (port 9090) - Metrics collection
3. ✅ **Grafana** (port 3001) - Visualization
4. ✅ **AlertManager** (port 9093) - Alerts
5. ✅ **Node Exporter** (port 9100) - System metrics
6. ✅ **Backend API** (port 8080) - Spring Boot
7. ✅ **Frontend** (port 5173) - React app

## Stopping Everything

### Linux/Mac
```bash
./stop-all.sh
```

### Windows
```cmd
stop-all.bat
```

## Troubleshooting

### Port Already in Use

If you see "port already in use" errors:

**Linux/Mac:**
```bash
# Kill process on port 8080 (backend)
lsof -ti:8080 | xargs kill -9

# Kill process on port 5173 (frontend)
lsof -ti:5173 | xargs kill -9
```

**Windows:**
```cmd
# Find process on port 8080
netstat -ano | findstr :8080

# Kill it (replace PID with actual number)
taskkill /PID <PID> /F
```

### Services Won't Start

1. **Check Docker is running**:
   ```bash
   docker ps
   ```

2. **Check logs**:
   ```bash
   # Backend
   tail -f logs/backend.log
   
   # Frontend
   tail -f logs/frontend.log
   
   # Docker
   docker-compose logs -f
   ```

3. **Restart Docker** and try again

### No Data in Monitoring

1. **Wait 30 seconds** for metrics to appear
2. **Generate test data**:
   ```bash
   for i in {1..50}; do
     curl http://localhost:8080/api/monitoring/health
   done
   ```
3. **Check Prometheus**: http://localhost:9090/targets

## Access URLs

### Main Application
| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | admin / password |
| Backend API | http://localhost:8080 | - |
| Health Check | http://localhost:8080/actuator/health | - |

### Monitoring
| Service | URL | Credentials |
|---------|-----|-------------|
| React Dashboard | http://localhost:5173/monitoring | admin / password |
| Grafana | http://localhost:3001 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| AlertManager | http://localhost:9093 | - |

### Database
| Service | Host | Port | Database | User | Password |
|---------|------|------|----------|------|----------|
| PostgreSQL | localhost | 5432 | personal_platform | admin | password |

## Next Steps

1. **Start the application** using the startup script
2. **Open your browser** to http://localhost:5173
3. **Login** with admin / password
4. **Explore the dashboard** and all features
5. **Check monitoring** at http://localhost:5173/monitoring
6. **View Grafana dashboards** at http://localhost:3001

## Documentation

- **STARTUP_GUIDE.md** - Detailed startup instructions
- **DEPLOYMENT_GUIDE.md** - Production deployment
- **MONITORING_ARCHITECTURE.md** - Monitoring system details
- **MONITORING_QUICKSTART.md** - Monitoring quick start
- **SETUP.md** - Detailed setup guide

## Features Overview

### Dashboard
- Portfolio summary with real-time stock prices
- Upcoming calendar events
- Recent posts and activities
- Quick action buttons

### Portfolio
- Add/edit/delete stock positions
- Real-time price updates via Finnhub API
- Performance metrics and gain/loss calculations
- Market hours awareness

### Calendar
- Google Calendar integration
- Event management (create/update/delete)
- Smart scheduling with conflict detection
- Multi-timezone support

### Content
- Social media-style posts
- Media tracking (books, movies, music, podcasts)
- Trip planning and activity logging
- Quick facts management

### Chat
- AI-powered assistant (requires Google Cloud setup)
- Natural language queries
- Context-aware responses
- Chat history

### Monitoring
- Real-time service health status
- Performance metrics (response times, error rates)
- Throttling and cache metrics
- Alert management
- Historical data analysis
- Data export capabilities

## Technology Stack

### Backend
- Spring Boot 3.3 with Java 21
- PostgreSQL database
- Micrometer & Prometheus for metrics
- WebSocket for real-time updates

### Frontend
- React 18 with TypeScript
- Vite for fast development
- Tailwind CSS for styling
- SockJS & STOMP for WebSocket

### Monitoring
- Prometheus for metrics collection
- Grafana for visualization
- AlertManager for alerting
- Node Exporter for system metrics

## Support

### Common Issues

1. **Port conflicts** - Stop other services using required ports
2. **Docker not running** - Start Docker Desktop
3. **Missing dependencies** - Install Java, Node.js, Docker
4. **Slow startup** - Normal on first run (downloading dependencies)

### Getting Help

1. Check **STARTUP_GUIDE.md** for detailed troubleshooting
2. Review logs in `logs/` directory
3. Check Docker container logs: `docker-compose logs -f`
4. Verify prerequisites are installed

## Quick Commands

```bash
# Start everything
./start-all.sh  # or start-all.bat on Windows

# Stop everything
./stop-all.sh   # or stop-all.bat on Windows

# View logs
tail -f logs/backend.log
tail -f logs/frontend.log

# Check Docker services
docker-compose ps
docker-compose -f docker-compose.monitoring.yml ps

# Check application health
curl http://localhost:8080/actuator/health
```

## Success Indicators

You'll know everything is working when:

- ✅ Frontend loads at http://localhost:5173
- ✅ You can login with admin / password
- ✅ Dashboard shows your data
- ✅ Monitoring page shows service health
- ✅ Grafana shows metrics at http://localhost:3001
- ✅ Prometheus shows targets UP at http://localhost:9090/targets

## 🎉 Ready to Start!

Run the startup script and enjoy your Personal Dashboard with comprehensive monitoring!

```bash
# Linux/Mac
./start-all.sh

# Windows
start-all.bat
```

**Questions?** Check STARTUP_GUIDE.md for detailed instructions.

**Happy monitoring!** 📊🚀
