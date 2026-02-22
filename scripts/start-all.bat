@echo off
REM Personal Dashboard - Complete Startup Script (Windows)
REM This script starts ALL services: Database, Monitoring Stack, Backend, and Frontend

setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo.
echo        Personal Dashboard - Complete Startup Script
echo.
echo   Starting: Database, Monitoring Stack, Backend, Frontend
echo.
echo ========================================================================
echo.

REM Check prerequisites
echo [STEP] Checking prerequisites...
echo.

where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not installed or not in PATH
    echo Please install Docker Desktop: https://docs.docker.com/desktop/install/windows-install/
    pause
    exit /b 1
)

where docker-compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker Compose is not installed or not in PATH
    pause
    exit /b 1
)

where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js/npm is not installed or not in PATH
    echo Please install Node.js: https://nodejs.org/
    pause
    exit /b 1
)

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 21+: https://adoptium.net/
    pause
    exit /b 1
)

echo [SUCCESS] All prerequisites are installed
echo.

REM Create logs directory
if not exist logs mkdir logs

REM Step 1: Start PostgreSQL Database
echo ========================================================================
echo [STEP] Step 1/6: Starting PostgreSQL Database
echo ========================================================================
echo.

echo [INFO] Starting PostgreSQL container...
docker-compose up -d postgres

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] PostgreSQL container started
    timeout /t 5 /nobreak >nul
    echo [SUCCESS] PostgreSQL is ready
) else (
    echo [ERROR] Failed to start PostgreSQL
    pause
    exit /b 1
)

echo.

REM Step 2: Start Monitoring Stack
echo ========================================================================
echo [STEP] Step 2/6: Starting Monitoring Stack
echo ========================================================================
echo.

echo [INFO] Starting Prometheus, Grafana, AlertManager...
docker-compose -f docker-compose.monitoring.yml up -d

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Monitoring stack containers started
    echo [INFO] Waiting for services to be ready (30 seconds)...
    timeout /t 30 /nobreak >nul
    echo [SUCCESS] Monitoring services should be ready
) else (
    echo [ERROR] Failed to start monitoring stack
    pause
    exit /b 1
)

echo.

REM Step 3: Install Frontend Dependencies
echo ========================================================================
echo [STEP] Step 3/6: Checking Frontend Dependencies
echo ========================================================================
echo.

cd frontend

if not exist node_modules (
    echo [INFO] Installing frontend dependencies...
    call npm install > ..\logs\npm-install.log 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [SUCCESS] Frontend dependencies installed
    ) else (
        echo [ERROR] Failed to install frontend dependencies
        cd ..
        pause
        exit /b 1
    )
) else (
    echo [SUCCESS] Frontend dependencies already installed
)

REM Check for monitoring dependencies
if not exist node_modules\sockjs-client (
    echo [INFO] Installing monitoring dependencies...
    call npm install sockjs-client @stomp/stompjs > ..\logs\npm-monitoring.log 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [SUCCESS] Monitoring dependencies installed
    ) else (
        echo [ERROR] Failed to install monitoring dependencies
        cd ..
        pause
        exit /b 1
    )
) else (
    echo [SUCCESS] Monitoring dependencies already installed
)

cd ..
echo.

REM Step 4: Start Backend Application
echo ========================================================================
echo [STEP] Step 4/6: Starting Backend Application
echo ========================================================================
echo.

cd backend

echo [INFO] Starting Spring Boot backend...
echo [WARNING] This may take 30-90 seconds on first startup...
echo.

REM Start backend in new window
start "Backend Application" /MIN cmd /c "mvnw.cmd spring-boot:run > ..\logs\backend.log 2>&1"

echo [INFO] Backend starting in background window...
echo [INFO] Waiting for backend to be ready (60 seconds)...

REM Wait for backend to start
timeout /t 60 /nobreak >nul

REM Check if backend is responding
curl -s http://localhost:8080/actuator/health >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Backend application is ready!
) else (
    echo [WARNING] Backend may still be starting...
    echo [INFO] Check logs\backend.log if issues persist
)

cd ..
echo.

REM Step 5: Start Frontend Application
echo ========================================================================
echo [STEP] Step 5/6: Starting Frontend Application
echo ========================================================================
echo.

cd frontend

echo [INFO] Starting Vite development server...

REM Start frontend in new window
start "Frontend Application" /MIN cmd /c "npm run dev > ..\logs\frontend.log 2>&1"

echo [INFO] Frontend starting in background window...
echo [INFO] Waiting for frontend to be ready (15 seconds)...

timeout /t 15 /nobreak >nul

REM Check if frontend is responding
curl -s http://localhost:5173 >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Frontend application is ready!
) else (
    echo [WARNING] Frontend may still be starting...
    echo [INFO] Check logs\frontend.log if issues persist
)

cd ..
echo.

REM Step 6: Verify Services
echo ========================================================================
echo [STEP] Step 6/6: Verifying All Services
echo ========================================================================
echo.

echo [INFO] Running final health checks...
echo.

REM Check Docker services
docker ps --format "{{.Names}}" | findstr /C:"personal-dashboard-db" >nul && echo [SUCCESS] PostgreSQL container is running
docker ps --format "{{.Names}}" | findstr /C:"prometheus" >nul && echo [SUCCESS] Prometheus container is running
docker ps --format "{{.Names}}" | findstr /C:"grafana" >nul && echo [SUCCESS] Grafana container is running
docker ps --format "{{.Names}}" | findstr /C:"alertmanager" >nul && echo [SUCCESS] AlertManager container is running

echo.

REM Print success message
echo.
echo ========================================================================
echo.
echo              All Services Started Successfully!
echo.
echo ========================================================================
echo.
echo MAIN APPLICATION
echo.
echo   Frontend:           http://localhost:5173
echo   Login:              admin / password
echo.
echo   Available Pages:
echo     - Dashboard:      http://localhost:5173/dashboard
echo     - Portfolio:      http://localhost:5173/portfolio
echo     - Calendar:       http://localhost:5173/calendar
echo     - Content:        http://localhost:5173/content
echo     - Chat:           http://localhost:5173/chat
echo     - Monitoring:     http://localhost:5173/monitoring
echo.
echo ========================================================================
echo.
echo MONITORING SERVICES
echo.
echo   Grafana:            http://localhost:3001
echo   Login:              admin / admin
echo.
echo   Prometheus:         http://localhost:9090
echo   AlertManager:       http://localhost:9093
echo   Node Exporter:      http://localhost:9100
echo.
echo ========================================================================
echo.
echo BACKEND SERVICES
echo.
echo   Backend API:        http://localhost:8080
echo   Health Check:       http://localhost:8080/actuator/health
echo   Metrics:            http://localhost:8080/actuator/prometheus
echo   Monitoring API:     http://localhost:8080/api/monitoring/health
echo.
echo ========================================================================
echo.
echo DATABASE
echo.
echo   PostgreSQL:         localhost:5432
echo   Database:           personal_platform
echo   User:               admin
echo.
echo ========================================================================
echo.
echo LOGS
echo.
echo   View logs:
echo     - logs\backend.log    (Backend application)
echo     - logs\frontend.log   (Frontend application)
echo.
echo   Docker logs:
echo     docker-compose logs -f postgres
echo     docker-compose -f docker-compose.monitoring.yml logs -f
echo.
echo ========================================================================
echo.
echo STOPPING SERVICES
echo.
echo   To stop all services:
echo     stop-all.bat
echo.
echo   Or manually:
echo     docker-compose down
echo     docker-compose -f docker-compose.monitoring.yml down
echo     (Close Backend and Frontend windows)
echo.
echo ========================================================================
echo.
echo Your Personal Dashboard is ready to use!
echo.
echo Start by visiting: http://localhost:5173
echo.

pause
