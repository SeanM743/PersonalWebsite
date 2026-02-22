# Personal Dashboard - Complete Startup Guide

## 🚀 Quick Start (Automated)

### Linux/Mac

```bash
# Make script executable
chmod +x start-all.sh

# Start everything
./start-all.sh
```

### Windows

```cmd
# Double-click or run from command prompt
start-all.bat
```

That's it! The script will:
1. ✅ Check all prerequisites
2. ✅ Start PostgreSQL database
3. ✅ Start monitoring stack (Prometheus, Grafana, AlertManager)
4. ✅ Install frontend dependencies (if needed)
5. ✅ Start backend application
6. ✅ Start frontend application
7. ✅ Verify all services are running

## 📋 Prerequisites

Before running the startup script, ensure you have:

### Required Software

1. **Docker & Docker Compose**
   - Download: https://docs.docker.com/get-docker/
   - Verify: `docker --version` and `docker-compose --version`

2. **Java 21+**
   - Download: https://adoptium.net/
   - Verify: `java -version`

3. **Node.js 18+ & npm**
   - Download: https://nodejs.org/
   - Verify: `node --version` and `npm --version`

4. **Git** (if cloning repository)
   - Download: https://git-scm.com/
   - Verify: `git --version`

### System Requirements

- **RAM**: 4GB minimum, 8GB recommended
- **Disk Space**: 5GB free space
- **Ports**: Ensure these ports are available:
  - 5432 (PostgreSQL)
  - 8080 (Backend API)
  - 5173 (Frontend)
  - 9090 (Prometheus)
  - 3001 (Grafana)
  - 9093 (AlertManager)
  - 9100 (Node Exporter)

## 🎯 What Gets Started

### 1. Database Layer
- **PostgreSQL** (port 5432)
  - Database: `personal_platform`
  - User: `admin`
  - Password: `password`

### 2. Monitoring Stack
- **Prometheus** (port 9090) - Metrics collection
- **Grafana** (port 3001) - Visualization
- **AlertManager** (port 9093) - Alert management
- **Node Exporter** (port 9100) - System metrics
- **cAdvisor** (port 8080) - Container metrics

### 3. Backend Application
- **Spring Boot API** (port 8080)
  - REST API endpoints
  - WebSocket server
  - Metrics collection
  - Health checks

### 4. Frontend Application
- **React + Vite** (port 5173)
  - Main dashboard
  - Portfolio management
  - Calendar integration
  - Content management
  - Chat interface
  - Monitoring dashboard

## 📊 Access Points

Once started, access your application at:

### Main Application
- **Frontend**: http://localhost:5173
- **Login**: `admin` / `password`

### Pages
- Dashboard: http://localhost:5173/dashboard
- Portfolio: http://localhost:5173/portfolio
- Calendar: http://localhost:5173/calendar
- Content: http://localhost:5173/content
- Chat: http://localhost:5173/chat
- **Monitoring**: http://localhost:5173/monitoring

### Monitoring Services
- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093

### Backend APIs
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Monitoring API**: http://localhost:8080/api/monitoring/health

## 🔧 Manual Startup (Step by Step)

If you prefer to start services manually or the script fails:

### Step 1: Start Database (30 seconds)

```bash
docker-compose up -d postgres

# Verify
docker ps | grep postgres
```

### Step 2: Start Monitoring Stack (1 minute)

```bash
docker-compose -f docker-compose.monitoring.yml up -d

# Verify
docker-compose -f docker-compose.monitoring.yml ps
```

Wait for all services to show "healthy" status.

### Step 3: Install Frontend Dependencies (2-5 minutes, first time only)

```bash
cd frontend

# Install all dependencies
npm install

# Install monitoring-specific dependencies
npm install sockjs-client @stomp/stompjs

cd ..
```

### Step 4: Start Backend (1-2 minutes)

```bash
cd backend

# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Wait for: `Started BackendApplication in X seconds`

Keep this terminal open or run in background:
```bash
# Linux/Mac (background)
nohup ./mvnw spring-boot:run > ../logs/backend.log 2>&1 &

# Windows (new window)
start cmd /c "mvnw.cmd spring-boot:run > ..\logs\backend.log 2>&1"
```

### Step 5: Start Frontend (30 seconds)

```bash
cd frontend

npm run dev
```

Wait for: `Local: http://localhost:5173/`

Keep this terminal open or run in background:
```bash
# Linux/Mac (background)
nohup npm run dev > ../logs/frontend.log 2>&1 &

# Windows (new window)
start cmd /c "npm run dev > ..\logs\frontend.log 2>&1"
```

### Step 6: Verify Everything Works

1. **Check Frontend**: http://localhost:5173
2. **Check Backend Health**: http://localhost:8080/actuator/health
3. **Check Prometheus**: http://localhost:9090/targets
4. **Check Grafana**: http://localhost:3001

## 🛑 Stopping Services

### Automated (Recommended)

```bash
# Linux/Mac
./stop-all.sh

# Windows
stop-all.bat
```

### Manual

```bash
# Stop frontend and backend (Ctrl+C in their terminals)
# Or if running in background:
kill $(cat backend.pid)
kill $(cat frontend.pid)

# Stop monitoring stack
docker-compose -f docker-compose.monitoring.yml down

# Stop database
docker-compose down
```

## 🐛 Troubleshooting

### Port Already in Use

**Problem**: Error message about port already in use

**Solution**:
```bash
# Linux/Mac - Find and kill process
lsof -ti:8080 | xargs kill -9  # Backend
lsof -ti:5173 | xargs kill -9  # Frontend

# Windows - Find and kill process
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Docker Services Won't Start

**Problem**: Docker containers fail to start

**Solution**:
```bash
# Check Docker is running
docker ps

# Restart Docker Desktop (Windows/Mac)
# Or restart Docker service (Linux)
sudo systemctl restart docker

# Remove old containers and try again
docker-compose down -v
docker-compose -f docker-compose.monitoring.yml down -v
```

### Backend Won't Start

**Problem**: Backend fails to start or takes too long

**Solutions**:

1. **Check Java version**:
   ```bash
   java -version  # Should be 21+
   ```

2. **Check database connection**:
   ```bash
   docker logs personal-dashboard-db
   ```

3. **Check backend logs**:
   ```bash
   tail -f logs/backend.log
   ```

4. **Common issues**:
   - Database not ready: Wait 10 more seconds
   - Port 8080 in use: Kill existing process
   - Maven dependencies: Delete `~/.m2/repository` and retry

### Frontend Won't Start

**Problem**: Frontend fails to start

**Solutions**:

1. **Check Node version**:
   ```bash
   node --version  # Should be 18+
   ```

2. **Reinstall dependencies**:
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   npm install sockjs-client @stomp/stompjs
   ```

3. **Check frontend logs**:
   ```bash
   tail -f logs/frontend.log
   ```

4. **Common issues**:
   - Port 5173 in use: Kill existing process
   - Missing dependencies: Run `npm install`
   - Build errors: Clear cache with `npm cache clean --force`

### No Data in Monitoring Dashboard

**Problem**: Monitoring dashboard shows no data

**Solutions**:

1. **Check Prometheus is scraping**:
   - Go to: http://localhost:9090/targets
   - Verify "digital-command-center" target is UP

2. **Check backend metrics endpoint**:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. **Generate test data**:
   ```bash
   for i in {1..50}; do
     curl http://localhost:8080/api/monitoring/health
     sleep 0.1
   done
   ```

4. **Wait for metrics to appear** (15-30 seconds)

### WebSocket Connection Fails

**Problem**: Real-time updates not working

**Solutions**:

1. **Check backend WebSocket endpoint**:
   ```bash
   curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" http://localhost:8080/ws
   ```

2. **Check CORS configuration**:
   - Verify `application.properties` has:
     ```properties
     security.cors.allowed-origins=http://localhost:5173
     ```

3. **Check browser console** (F12) for errors

4. **Verify authentication**:
   - Login to the application first
   - Check JWT token in localStorage

## 📝 Logs

### View Logs

```bash
# Backend logs
tail -f logs/backend.log

# Frontend logs
tail -f logs/frontend.log

# Docker logs
docker-compose logs -f postgres
docker-compose -f docker-compose.monitoring.yml logs -f prometheus
docker-compose -f docker-compose.monitoring.yml logs -f grafana
```

### Log Locations

- Backend: `logs/backend.log`
- Frontend: `logs/frontend.log`
- Docker: Use `docker logs <container-name>`

## 🔍 Verifying Services

### Check All Services Status

```bash
# Docker services
docker-compose ps
docker-compose -f docker-compose.monitoring.yml ps

# Backend process
ps aux | grep java

# Frontend process
ps aux | grep node

# All ports
netstat -tuln | grep -E '5432|8080|5173|9090|3001|9093'
```

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Prometheus health
curl http://localhost:9090/-/healthy

# Grafana health
curl http://localhost:3001/api/health

# AlertManager health
curl http://localhost:9093/-/healthy
```

## 🎓 First Time Setup

### 1. Clone Repository (if needed)

```bash
git clone <repository-url>
cd personal_webpage
```

### 2. Configure Environment (optional)

```bash
# Copy example environment file
cp .env.example .env

# Edit with your settings
nano .env
```

### 3. Run Startup Script

```bash
# Linux/Mac
chmod +x start-all.sh
./start-all.sh

# Windows
start-all.bat
```

### 4. Access Application

1. Open browser: http://localhost:5173
2. Login: `admin` / `password`
3. Explore the dashboard!

## 🚀 Production Deployment

For production deployment, see:
- **DEPLOYMENT_GUIDE.md** - Complete production deployment guide
- **ENTERPRISE_DEPLOYMENT_PATTERNS.md** - Kubernetes and enterprise patterns

## 💡 Tips

1. **First startup takes longer** (downloading dependencies)
2. **Subsequent startups are faster** (cached dependencies)
3. **Keep logs directory** for troubleshooting
4. **Use automated scripts** for consistency
5. **Check prerequisites** before starting
6. **Monitor resource usage** (RAM, CPU, disk)

## 📞 Getting Help

1. Check this guide's troubleshooting section
2. Review logs in `logs/` directory
3. Check Docker container logs
4. Verify all prerequisites are installed
5. Ensure all required ports are available

## 🎉 Success!

Once all services are running, you should see:

- ✅ Frontend accessible at http://localhost:5173
- ✅ Backend responding at http://localhost:8080
- ✅ Prometheus collecting metrics at http://localhost:9090
- ✅ Grafana showing dashboards at http://localhost:3001
- ✅ All Docker containers healthy

**Enjoy your Personal Dashboard!** 🚀
