#!/bin/bash

# Personal Dashboard - Complete Startup Script
# This script starts ALL services: Database, Monitoring Stack, Backend, and Frontend

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Function to get the correct docker compose command
get_docker_compose_cmd() {
    # Test docker compose (new format) first
    if docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    # Test docker-compose (legacy format) second
    elif command_exists "docker-compose" && docker-compose --version >/dev/null 2>&1; then
        echo "docker-compose"
    else
        print_error "Neither 'docker compose' nor 'docker-compose' is working"
        print_error "Please enable WSL integration in Docker Desktop settings"
        print_error "Go to Docker Desktop → Settings → Resources → WSL Integration"
        exit 1
    fi
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i ":$1" >/dev/null 2>&1
}

# Function to wait for a service to be ready
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=60
    local attempt=0

    print_status "Waiting for $name to be ready..."
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" >/dev/null 2>&1; then
            print_success "$name is ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        printf "."
        sleep 2
    done
    
    echo ""
    print_error "$name failed to start within expected time"
    return 1
}

# Function to check Docker service health
check_docker_health() {
    local service=$1
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        local health=$(docker inspect --format='{{.State.Health.Status}}' $service 2>/dev/null || echo "none")
        if [ "$health" = "healthy" ]; then
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    return 1
}

# Print banner
clear
echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║        🚀 Personal Dashboard - Complete Startup Script 🚀        ║"
echo "║                                                                  ║"
echo "║  Starting: Database, Monitoring Stack, Backend, Frontend        ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Check prerequisites
print_step "Checking prerequisites..."
echo ""

MISSING_DEPS=()

if ! command_exists docker; then
    MISSING_DEPS+=("Docker")
fi

# Get the correct docker compose command
DOCKER_COMPOSE_CMD=$(get_docker_compose_cmd)
print_success "Using Docker Compose command: $DOCKER_COMPOSE_CMD"

if ! command_exists npm; then
    MISSING_DEPS+=("Node.js/npm")
fi

if ! command_exists java; then
    MISSING_DEPS+=("Java")
fi

if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    print_error "Missing required dependencies: ${MISSING_DEPS[*]}"
    echo ""
    echo "Please install the missing dependencies:"
    echo "  - Docker: https://docs.docker.com/get-docker/"
    echo "  - Docker Compose: https://docs.docker.com/compose/install/"
    echo "  - Node.js: https://nodejs.org/"
    echo "  - Java 21+: https://adoptium.net/"
    echo ""
    exit 1
fi

print_success "All prerequisites are installed"
echo "  ✓ Docker: $(docker --version | cut -d' ' -f3 | cut -d',' -f1)"
echo "  ✓ Docker Compose: $($DOCKER_COMPOSE_CMD --version | cut -d' ' -f4 | cut -d',' -f1 2>/dev/null || echo 'Available')"
echo "  ✓ Node.js: $(node --version)"
echo "  ✓ Java: $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"
echo ""

# Check if ports are available
print_step "Checking required ports..."
echo ""

REQUIRED_PORTS=(
    "5432:PostgreSQL"
    "8080:Backend API"
    "5174:Frontend"
    "9090:Prometheus"
    "3001:Grafana"
    "9093:AlertManager"
    "9100:Node Exporter"
)

PORTS_IN_USE=()

for port_info in "${REQUIRED_PORTS[@]}"; do
    port=$(echo $port_info | cut -d':' -f1)
    service=$(echo $port_info | cut -d':' -f2)
    
    if port_in_use $port; then
        PORTS_IN_USE+=("$port ($service)")
        print_warning "Port $port is in use ($service)"
    else
        print_success "Port $port is available ($service)"
    fi
done

if [ ${#PORTS_IN_USE[@]} -gt 0 ]; then
    echo ""
    print_warning "Some ports are already in use. This might cause conflicts."
    echo ""
    echo "Ports in use: ${PORTS_IN_USE[*]}"
    echo ""
    echo "Do you want to continue anyway? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        print_status "Startup cancelled by user"
        exit 0
    fi
fi

echo ""

# Create logs directory
mkdir -p logs

# Step 1: Start PostgreSQL Database
print_step "Step 1/6: Starting PostgreSQL Database"
echo ""

print_status "Starting PostgreSQL container..."
$DOCKER_COMPOSE_CMD up -d postgres

if [ $? -eq 0 ]; then
    print_success "PostgreSQL container started"
    
    # Wait for PostgreSQL to be ready
    print_status "Waiting for PostgreSQL to accept connections..."
    sleep 5
    
    if docker exec personal-dashboard-db pg_isready -U admin >/dev/null 2>&1; then
        print_success "PostgreSQL is ready and accepting connections"
    else
        print_warning "PostgreSQL may still be initializing (this is normal on first run)"
    fi
else
    print_error "Failed to start PostgreSQL"
    exit 1
fi

echo ""

# Step 2: Start Monitoring Stack
print_step "Step 2/6: Starting Monitoring Stack"
echo ""

print_status "Starting Prometheus, Grafana, AlertManager, and exporters..."
$DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml up -d

if [ $? -eq 0 ]; then
    print_success "Monitoring stack containers started"
    
    # Wait for each service
    echo ""
    print_status "Waiting for monitoring services to be healthy..."
    
    wait_for_service "http://localhost:9090/-/healthy" "Prometheus"
    wait_for_service "http://localhost:3001/api/health" "Grafana"
    wait_for_service "http://localhost:9093/-/healthy" "AlertManager"
    wait_for_service "http://localhost:9100/metrics" "Node Exporter"
    
    print_success "All monitoring services are ready"
else
    print_error "Failed to start monitoring stack"
    exit 1
fi

echo ""

# Step 3: Install/Check Frontend Dependencies
print_step "Step 3/6: Checking Frontend Dependencies"
echo ""

cd frontend

if [ ! -d "node_modules" ]; then
    print_status "Installing frontend dependencies (this may take a few minutes)..."
    npm install > ../logs/npm-install.log 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Frontend dependencies installed"
    else
        print_error "Failed to install frontend dependencies. Check logs/npm-install.log"
        cd ..
        exit 1
    fi
else
    print_success "Frontend dependencies already installed"
fi

# Check for monitoring-specific dependencies
if [ ! -d "node_modules/sockjs-client" ] || [ ! -d "node_modules/@stomp" ]; then
    print_status "Installing monitoring dependencies..."
    npm install sockjs-client @stomp/stompjs > ../logs/npm-monitoring.log 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Monitoring dependencies installed"
    else
        print_error "Failed to install monitoring dependencies"
        cd ..
        exit 1
    fi
else
    print_success "Monitoring dependencies already installed"
fi

cd ..
echo ""

# Step 4: Start Backend Application
print_step "Step 4/6: Starting Backend Application"
echo ""

cd backend

# Check if Maven wrapper exists
if [ ! -f "mvnw" ]; then
    print_error "Maven wrapper not found. Please ensure you're in the correct directory."
    exit 1
fi

# Make Maven wrapper executable
chmod +x mvnw

print_status "Starting Spring Boot backend..."
print_warning "This may take 30-90 seconds on first startup (downloading dependencies)..."
echo ""

# Start backend in background
nohup ./mvnw spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../backend.pid

print_status "Backend starting with PID: $BACKEND_PID"

cd ..

# Wait for backend to be ready
print_status "Waiting for backend to start (checking health endpoint)..."
sleep 15  # Give it some initial time to start

if wait_for_service "http://localhost:8080/actuator/health" "Backend API"; then
    print_success "Backend application is ready!"
    
    # Verify Prometheus endpoint
    if curl -s "http://localhost:8080/actuator/prometheus" >/dev/null 2>&1; then
        print_success "Prometheus metrics endpoint is accessible"
    else
        print_warning "Prometheus metrics endpoint may not be ready yet"
    fi
else
    print_error "Backend failed to start within expected time"
    echo ""
    print_status "Last 20 lines of backend log:"
    tail -n 20 logs/backend.log
    echo ""
    print_error "Check logs/backend.log for full details"
    exit 1
fi

echo ""

# Step 5: Start Frontend Application
print_step "Step 5/6: Starting Frontend Application"
echo ""

cd frontend

print_status "Starting Vite development server..."

# Start frontend in background
nohup npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > ../frontend.pid

print_status "Frontend starting with PID: $FRONTEND_PID"

cd ..

# Wait for frontend to be ready
sleep 8
if wait_for_service "http://localhost:5174" "Frontend"; then
    print_success "Frontend application is ready!"
else
    print_error "Frontend failed to start within expected time"
    echo ""
    print_status "Last 20 lines of frontend log:"
    tail -n 20 logs/frontend.log
    echo ""
    print_error "Check logs/frontend.log for full details"
    exit 1
fi

echo ""

# Step 6: Verify All Services
print_step "Step 6/6: Verifying All Services"
echo ""

print_status "Running final health checks..."
echo ""

# Check Docker services
DOCKER_SERVICES=(
    "personal-dashboard-db:PostgreSQL"
    "prometheus:Prometheus"
    "grafana:Grafana"
    "alertmanager:AlertManager"
    "node-exporter:Node Exporter"
)

for service_info in "${DOCKER_SERVICES[@]}"; do
    service=$(echo $service_info | cut -d':' -f1)
    name=$(echo $service_info | cut -d':' -f2)
    
    if docker ps --format '{{.Names}}' | grep -q "^${service}$"; then
        print_success "$name container is running"
    else
        print_warning "$name container may not be running"
    fi
done

echo ""

# Check application endpoints
ENDPOINTS=(
    "http://localhost:8080/actuator/health:Backend Health"
    "http://localhost:8080/actuator/prometheus:Backend Metrics"
    "http://localhost:5174:Frontend"
    "http://localhost:9090/-/healthy:Prometheus"
    "http://localhost:3001/api/health:Grafana"
    "http://localhost:9093/-/healthy:AlertManager"
)

for endpoint_info in "${ENDPOINTS[@]}"; do
    endpoint=$(echo $endpoint_info | cut -d':' -f1,2,3)
    name=$(echo $endpoint_info | cut -d':' -f4)
    
    if curl -s "$endpoint" >/dev/null 2>&1; then
        print_success "$name is responding"
    else
        print_warning "$name may not be ready yet"
    fi
done

echo ""

# Print success message and access information
echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║              🎉 All Services Started Successfully! 🎉            ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}📱 MAIN APPLICATION${NC}"
echo ""
echo -e "  🌐 Frontend:           ${GREEN}http://localhost:5174${NC}"
echo -e "  🔐 Login:              ${YELLOW}admin / password${NC}"
echo ""
echo "  Available Pages:"
echo "    • Dashboard:         http://localhost:5174/dashboard"
echo "    • Portfolio:         http://localhost:5174/portfolio"
echo "    • Calendar:          http://localhost:5174/calendar"
echo "    • Content:           http://localhost:5174/content"
echo "    • Chat:              http://localhost:5174/chat"
echo -e "    • ${CYAN}Monitoring:${NC}        ${GREEN}http://localhost:5174/monitoring${NC}"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}📊 MONITORING SERVICES${NC}"
echo ""
echo -e "  📈 Grafana:            ${GREEN}http://localhost:3001${NC}"
echo -e "  🔐 Login:              ${YELLOW}admin / admin${NC}"
echo ""
echo -e "  🔍 Prometheus:         ${GREEN}http://localhost:9090${NC}"
echo -e "  🚨 AlertManager:       ${GREEN}http://localhost:9093${NC}"
echo -e "  📊 Node Exporter:      ${GREEN}http://localhost:9100${NC}"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}🔧 BACKEND SERVICES${NC}"
echo ""
echo -e "  🔌 Backend API:        ${GREEN}http://localhost:8080${NC}"
echo "  ❤️  Health Check:       http://localhost:8080/actuator/health"
echo "  📊 Metrics:            http://localhost:8080/actuator/prometheus"
echo "  📡 Monitoring API:     http://localhost:8080/api/monitoring/health"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}💾 DATABASE${NC}"
echo ""
echo -e "  🐘 PostgreSQL:         ${GREEN}localhost:5432${NC}"
echo "  📦 Database:           personal_platform"
echo "  👤 User:               admin"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}📝 PROCESS INFORMATION${NC}"
echo ""
echo "  Backend PID:           $BACKEND_PID"
echo "  Frontend PID:          $FRONTEND_PID"
echo ""
echo "  PIDs saved to:"
echo "    • backend.pid"
echo "    • frontend.pid"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}📋 LOGS${NC}"
echo ""
echo "  View logs with:"
echo -e "    ${YELLOW}tail -f logs/backend.log${NC}   - Backend application logs"
echo -e "    ${YELLOW}tail -f logs/frontend.log${NC}  - Frontend application logs"
echo ""
echo "  Docker logs:"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD logs -f postgres${NC}"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml logs -f${NC}"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}🛑 STOPPING SERVICES${NC}"
echo ""
echo "  To stop all services:"
echo -e "    ${YELLOW}./stop-all.sh${NC}"
echo ""
echo "  Or manually:"
echo -e "    ${YELLOW}kill \$(cat backend.pid)${NC}   - Stop backend"
echo -e "    ${YELLOW}kill \$(cat frontend.pid)${NC}  - Stop frontend"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD down${NC}        - Stop database"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml down${NC}"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${MAGENTA}🔍 CHECKING STATUS${NC}"
echo ""
echo "  Check Docker services:"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD ps${NC}"
echo -e "    ${YELLOW}$DOCKER_COMPOSE_CMD -f docker-compose.monitoring.yml ps${NC}"
echo ""
echo "  Check application processes:"
echo -e "    ${YELLOW}ps aux | grep java${NC}    - Backend process"
echo -e "    ${YELLOW}ps aux | grep node${NC}    - Frontend process"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}✨ Your Personal Dashboard is ready to use!${NC}"
echo ""
echo -e "Start by visiting: ${GREEN}http://localhost:5174${NC}"
echo ""
