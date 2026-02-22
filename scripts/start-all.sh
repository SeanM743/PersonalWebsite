#!/bin/bash

# Personal Dashboard - Docker Startup Script
# Starts all services using Docker Compose

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[✓]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[⚠]${NC} $1"; }
print_error() { echo -e "${RED}[✗]${NC} $1"; }

# Check Docker
if ! command -v docker >/dev/null 2>&1; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║          🚀 Personal Dashboard - Docker Startup 🚀              ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Create directories
mkdir -p logs volume

# 1. Start Main Applications (DB, Backend, Frontend)
print_status "Starting Database, Backend, and Frontend..."
docker compose up -d --build

# 2. Start Monitoring Stack
print_status "Starting Monitoring Stack..."
docker compose -f docker-compose.monitoring.yml up -d

echo ""
print_status "Waiting for services to become healthy..."

# Helper to check health
wait_for_url() {
    local url=$1
    local name=$2
    local max_tries=30
    local i=0
    
    echo -n "Waiting for $name..."
    while [ $i -lt $max_tries ]; do
        if curl -s "$url" >/dev/null 2>&1; then
            echo -e " ${GREEN}OK${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        i=$((i+1))
    done
    echo -e " ${RED}Failed${NC}"
    return 1
}

wait_for_url "http://localhost:5174" "Frontend"
wait_for_url "http://localhost:8080/actuator/health" "Backend"
wait_for_url "http://localhost:9090/-/healthy" "Prometheus"

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║              🎉 All Systems Operational! 🎉                      ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "  🌐 Frontend:    ${GREEN}http://localhost:5174${NC}"
echo -e "  🔌 Backend:     ${GREEN}http://localhost:8080${NC}"
echo -e "  🔍 Prometheus:  ${GREEN}http://localhost:9090${NC}"
echo ""
echo "Logs preserved in Docker:"
echo "  docker compose logs -f [backend|frontend|postgres]"
echo ""
