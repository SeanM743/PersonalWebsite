#!/bin/bash

# Personal Dashboard - Complete Shutdown Script
# This script stops ALL services: Frontend, Backend, Monitoring Stack, and Database

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to get the correct docker compose command
get_docker_compose_cmd() {
    # Test docker compose (new format) first
    if docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    # Test docker-compose (legacy format) second
    elif command_exists "docker-compose" && docker-compose --version >/dev/null 2>&1; then
        echo "docker-compose"
    else
        echo "docker compose"  # Default fallback
    fi
}

# Print banner
clear
echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║        🛑 Personal Dashboard - Complete Shutdown Script 🛑       ║"
echo "║                                                                  ║"
echo "║  Stopping: Frontend, Backend, Monitoring Stack, Database        ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Get the correct docker compose command
DOCKER_COMPOSE_CMD=$(get_docker_compose_cmd)

# Step 1: Stop Frontend
print_step "Step 1/4: Stopping Frontend Application"
echo ""

if [ -f "frontend.pid" ]; then
    FRONTEND_PID=$(cat frontend.pid)
    print_status "Stopping frontend (PID: $FRONTEND_PID)..."
    
    if kill $FRONTEND_PID 2>/dev/null; then
        print_success "Frontend stopped"
        rm frontend.pid
    else
        print_warning "Frontend process not found (may have already stopped)"
        rm -f frontend.pid
    fi
else
    print_warning "No frontend PID file found"
fi

# Also try to kill any remaining node processes running vite
pkill -f "vite" 2>/dev/null && print_status "Cleaned up any remaining Vite processes"

echo ""

# Step 2: Stop Backend
print_step "Step 2/4: Stopping Backend Application"
echo ""

if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    print_status "Stopping backend (PID: $BACKEND_PID)..."
    
    if kill $BACKEND_PID 2>/dev/null; then
        print_success "Backend stopped"
        rm backend.pid
    else
        print_warning "Backend process not found (may have already stopped)"
        rm -f backend.pid
    fi
else
    print_warning "No backend PID file found"
fi

# Also try to kill any remaining Spring Boot processes
pkill -f "spring-boot:run" 2>/dev/null && print_status "Cleaned up any remaining Spring Boot processes"

echo ""

# Step 3: Stop Monitoring Stack
print_step "Step 3/4: Stopping Monitoring Stack"
echo ""

print_status "Stopping Prometheus, Grafana, AlertManager, and exporters..."
$DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml down

if [ $? -eq 0 ]; then
    print_success "Monitoring stack stopped"
else
    print_warning "Some monitoring services may not have stopped cleanly"
fi

echo ""

# Step 4: Stop Database
print_step "Step 4/4: Stopping PostgreSQL Database"
echo ""

print_status "Stopping PostgreSQL..."
$DOCKER_COMPOSE_CMD down

if [ $? -eq 0 ]; then
    print_success "PostgreSQL stopped"
else
    print_warning "Database may not have stopped cleanly"
fi

echo ""

# Clean up log files (optional)
print_status "Log files are preserved in the logs/ directory"
echo ""

# Verify all services are stopped
print_step "Verifying all services are stopped..."
echo ""

# Check for any remaining processes
REMAINING_PROCESSES=()

if pgrep -f "spring-boot:run" >/dev/null; then
    REMAINING_PROCESSES+=("Backend (Spring Boot)")
fi

if pgrep -f "vite" >/dev/null; then
    REMAINING_PROCESSES+=("Frontend (Vite)")
fi

# Check Docker containers
if docker ps --format '{{.Names}}' | grep -q "prometheus\|grafana\|alertmanager\|personal-dashboard-db"; then
    REMAINING_PROCESSES+=("Docker containers")
fi

if [ ${#REMAINING_PROCESSES[@]} -gt 0 ]; then
    print_warning "Some processes may still be running: ${REMAINING_PROCESSES[*]}"
    echo ""
    echo "To force stop all processes:"
    echo "  pkill -9 -f 'spring-boot:run'"
    echo "  pkill -9 -f 'vite'"
    echo "  $DOCKER_COMPOSE_CMD down -v"
    echo "  $DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml down -v"
else
    print_success "All services stopped successfully"
fi

echo ""

# Print summary
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║              ✅ All Services Stopped Successfully! ✅             ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "${GREEN}Services Stopped:${NC}"
echo "  ✓ Frontend Application"
echo "  ✓ Backend Application"
echo "  ✓ Monitoring Stack (Prometheus, Grafana, AlertManager)"
echo "  ✓ PostgreSQL Database"
echo ""
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "${YELLOW}Logs Preserved:${NC}"
echo "  • logs/backend.log"
echo "  • logs/frontend.log"
echo ""
echo "To view logs:"
echo "  cat logs/backend.log"
echo "  cat logs/frontend.log"
echo ""
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "${GREEN}To start services again:${NC}"
echo "  ./start-all.sh"
echo ""
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "${GREEN}Data Preserved:${NC}"
echo "  • Database data (Docker volume)"
echo "  • Prometheus data (Docker volume)"
echo "  • Grafana dashboards (Docker volume)"
echo ""
echo "To completely remove all data:"
echo "  $DOCKER_COMPOSE_CMD down -v"
echo "  $DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml down -v"
echo ""
