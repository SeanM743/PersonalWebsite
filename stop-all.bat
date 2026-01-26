@echo off
REM Personal Dashboard - Complete Shutdown Script (Windows)
REM This script stops ALL services: Frontend, Backend, Monitoring Stack, and Database

echo.
echo ========================================================================
echo.
echo        Personal Dashboard - Complete Shutdown Script
echo.
echo   Stopping: Frontend, Backend, Monitoring Stack, Database
echo.
echo ========================================================================
echo.

REM Step 1: Stop Frontend
echo [STEP] Step 1/4: Stopping Frontend Application
echo.

taskkill /FI "WINDOWTITLE eq Frontend Application*" /F >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Frontend stopped
) else (
    echo [WARNING] Frontend process not found ^(may have already stopped^)
)

REM Also kill any node processes running vite
taskkill /F /IM node.exe /FI "COMMANDLINE eq *vite*" >nul 2>nul

echo.

REM Step 2: Stop Backend
echo [STEP] Step 2/4: Stopping Backend Application
echo.

taskkill /FI "WINDOWTITLE eq Backend Application*" /F >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Backend stopped
) else (
    echo [WARNING] Backend process not found ^(may have already stopped^)
)

REM Also kill any Java processes running Spring Boot
taskkill /F /IM java.exe /FI "COMMANDLINE eq *spring-boot*" >nul 2>nul

echo.

REM Step 3: Stop Monitoring Stack
echo [STEP] Step 3/4: Stopping Monitoring Stack
echo.

echo [INFO] Stopping Prometheus, Grafana, AlertManager...
docker-compose -f docker-compose.monitoring.yml down

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Monitoring stack stopped
) else (
    echo [WARNING] Some monitoring services may not have stopped cleanly
)

echo.

REM Step 4: Stop Database
echo [STEP] Step 4/4: Stopping PostgreSQL Database
echo.

echo [INFO] Stopping PostgreSQL...
docker-compose down

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] PostgreSQL stopped
) else (
    echo [WARNING] Database may not have stopped cleanly
)

echo.

REM Print summary
echo ========================================================================
echo.
echo              All Services Stopped Successfully!
echo.
echo ========================================================================
echo.
echo Services Stopped:
echo   - Frontend Application
echo   - Backend Application
echo   - Monitoring Stack ^(Prometheus, Grafana, AlertManager^)
echo   - PostgreSQL Database
echo.
echo ========================================================================
echo.
echo Logs Preserved:
echo   - logs\backend.log
echo   - logs\frontend.log
echo.
echo To view logs:
echo   type logs\backend.log
echo   type logs\frontend.log
echo.
echo ========================================================================
echo.
echo To start services again:
echo   start-all.bat
echo.
echo ========================================================================
echo.
echo Data Preserved:
echo   - Database data ^(Docker volume^)
echo   - Prometheus data ^(Docker volume^)
echo   - Grafana dashboards ^(Docker volume^)
echo.
echo To completely remove all data:
echo   docker-compose down -v
echo   docker-compose -f docker-compose.monitoring.yml down -v
echo.

pause
