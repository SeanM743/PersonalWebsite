#!/bin/bash

# Service Health Dashboard - Startup Script
# This script starts all required services for the monitoring system

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
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
    local max_attempts=30
    local attempt=0

    print_status "Waiting for $name to be ready..."
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" >/dev/null 2>&1; then
            print_success "$name is ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    
    print_error "$name failed to start within expected time"
    return 1
}

# Print banner
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Service Health Dashboard - Startup Script             ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check prerequisites
print_status "Checking prerequisites..."

if ! command_exists docker; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command_exists docker-compose; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

if ! command_exists npm; then
    print_error "npm is not installed. Please install Node.js and npm first."
    exit 1
fi

if ! command_exists java; then
    print_error "Java is not installed. Please install Java 21+ first."
    exit 1
fi

print_success "All prerequisites are installed"
echo ""

# Check if ports are available
print_status "Checking if required ports are available..."

PORTS_TO_CHECK=(5432 8080 5173 9090 3001 9093)
PORTS_IN_USE=()

for port in "${PORTS_TO_CHECK[@]}"; do
    if port_in_use $port; then
        PORTS_IN_USE+=($port)
    fi
done

if [ ${#PORTS_IN_USE[@]} -gt 0 ]; then
    print_warning "The following ports are already in use: ${PORTS_IN_USE[*]}"
    echo "This might cause conflicts. Do you want to continue? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        print_status "Startup cancelled"
        exit 0
    fi
fi

print_success "Port check complete"
echo ""

# Step 1: Start Database
print_status "Step 1/5: Starting PostgreSQL database..."
docker-compose up -d postgres

if [ $? -eq 0 ]; then
    print_success "PostgreSQL started successfully"
    wait_for_service "http://localhost:5432" "PostgreSQL" || true
else
    print_error "Failed to start PostgreSQL"
    exit 1
fi
echo ""

# Step 2: Start Monitoring Stack
print_status "Step 2/5: Starting monitoring stack (Prometheus, Grafana, AlertManager)..."
docker-compose -f docker-compose.monitoring.yml up -d

if [ $? -eq 0 ]; then
    print_success "Monitoring stack started successfully"
    
    # Wait for services to be ready
    wait_for_service "http://localhost:9090/-/healthy" "Prometheus"
    wait_for_service "http://localhost:3001/api/health" "Grafana"
    wait_for_service "http://localhost:9093/-/healthy" "AlertManager"
else
    print_error "Failed to start monitoring stack"
    exit 1
fi
echo ""

# Step 3: Install Frontend Dependencies (if needed)
print_status "Step 3/5: Checking frontend dependencies..."
cd frontend

if [ ! -d "node_modules" ] || [ ! -d "node_modules/sockjs-client" ] || [ ! -d "node_modules/@stomp" ]; then
    print_status "Installing frontend dependencies..."
    npm install
    npm install sockjs-client @stomp/stompjs
    print_success "Frontend dependencies installed"
else
    print_success "Frontend dependencies already installed"
fi

cd ..
echo ""

# Step 4: Start Backend
print_status "Step 4/5: Starting backend application..."
print_warning "This may take 30-60 seconds for the first startup..."

cd backend

# Check if Maven wrapper exists
if [ ! -f "mvnw" ]; then
    print_error "Maven wrapper not found. Please ensure you're in the correct directory."
    exit 1
fi

# Make Maven wrapper executable
chmod +x mvnw

# Start backend in background
./mvnw spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!

cd ..

# Wait for backend to be ready
print_status "Waiting for backend to start (this may take a minute)..."
sleep 10  # Give it some initial time

if wait_for_service "http://localhost:8080/actuator/health" "Backend"; then
    print_success "Backend started successfully (PID: $BACKEND_PID)"
    echo $BACKEND_PID > backend.pid
else
    print_error "Backend failed to start. Check backend.log for details."
    exit 1
fi
echo ""

# Step 5: Start Frontend
print_status "Step 5/5: Starting frontend application..."

cd frontend

# Start frontend in background
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!

cd ..

# Wait for frontend to be ready
sleep 5
if wait_for_service "http://localhost:5173" "Frontend"; then
    print_success "Frontend started successfully (PID: $FRONTEND_PID)"
    echo $FRONTEND_PID > frontend.pid
else
    print_error "Frontend failed to start. Check frontend.log for details."
    exit 1
fi
echo ""

# Print success message and access information
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                  🎉 All Services Started! 🎉               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Access your monitoring system:"
echo ""
echo "  📊 React Dashboard:    http://localhost:5173/monitoring"
echo "  🔐 Login:              admin / password"
echo ""
echo "  📈 Grafana:            http://localhost:3001"
echo "  🔐 Login:              admin / admin"
echo ""
echo "  🔍 Prometheus:         http://localhost:9090"
echo "  🚨 AlertManager:       http://localhost:9093"
echo "  🔧 Backend API:        http://localhost:8080"
echo ""
echo "Process IDs:"
echo "  Backend PID:  $BACKEND_PID"
echo "  Frontend PID: $FRONTEND_PID"
echo ""
echo "Logs:"
echo "  Backend:  tail -f backend.log"
echo "  Frontend: tail -f frontend.log"
echo ""
echo "To stop all services, run:"
echo "  ./stop-monitoring.sh"
echo ""
echo "To view service status:"
echo "  docker-compose ps"
echo "  docker-compose -f docker-compose.monitoring.yml ps"
echo ""
print_success "Monitoring system is ready to use!"
echo ""
