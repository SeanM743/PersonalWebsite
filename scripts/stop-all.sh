#!/bin/bash

# Personal Dashboard - Docker Shutdown Script

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[✓]${NC} $1"; }

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║             🛑 STOPPING ALL DOCKER SERVICES 🛑                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

print_status "Stopping Monitoring Stack..."
docker compose -f docker-compose.monitoring.yml down
print_success "Monitoring stopped"

echo ""

print_status "Stopping Main Applications (Backend, Frontend, DB)..."
docker compose down
print_success "Main apps stopped"

# Cleanup any PID files just in case
rm -f backend.pid frontend.pid

echo ""
echo -e "${GREEN}All services shut down successfully.${NC}"
echo ""
