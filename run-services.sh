#!/bin/bash

# Maven Wrapper Script for Loan Origination System
# This script manages all microservices

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.services.pid"
LOG_DIR="$SCRIPT_DIR/logs"

# Service modules (excluding common)
SERVICES=(
    "api-gateway:8080"
    "orchestrator:8081"
    "decision-engine:8082"
    "experian-connector:8083"
    "equifax-connector:8084"
    "audit-logging:8085"
    "mcp-server:8086"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create logs directory
mkdir -p "$LOG_DIR"

# Function to check if a port is in use
check_port() {
    local port=$1
    lsof -i :$port > /dev/null 2>&1
}

# Function to wait for service to be ready
wait_for_service() {
    local port=$1
    local service_name=$2
    local max_attempts=30
    local attempt=0
    
    echo -n "Waiting for $service_name to start on port $port"
    while [ $attempt -lt $max_attempts ]; do
        if check_port $port; then
            echo -e " ${GREEN}✓${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo -e " ${RED}✗${NC} (timeout)"
    return 1
}

# Function to start all services
start_services() {
    echo -e "${GREEN}Starting Loan Origination System Services...${NC}"
    echo ""
    
    # Check if services are already running
    if [ -f "$PID_FILE" ]; then
        echo -e "${YELLOW}Services appear to be already running. Use 'stop' command first.${NC}"
        return 1
    fi
    
    # Check if Docker services are running
    if ! docker ps > /dev/null 2>&1; then
        echo -e "${YELLOW}Warning: Docker daemon is not running. Please start Docker first.${NC}"
        echo "Run: docker-compose up -d"
    fi
    
    # Create PID file
    touch "$PID_FILE"
    
    # Start each service
    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r module port <<< "$service_info"
        service_name="${module//-/_}"
        
        echo -e "${GREEN}Starting $module...${NC}"
        
        # Start service in background and capture PID
        cd "$SCRIPT_DIR"
        nohup mvn spring-boot:run -pl "$module" > "$LOG_DIR/$module.log" 2>&1 &
        PID=$!
        echo "$PID:$module:$port" >> "$PID_FILE"
        
        # Wait a bit before starting next service
        sleep 3
    done
    
    echo ""
    echo -e "${GREEN}All services started!${NC}"
    echo "Check logs in: $LOG_DIR"
    echo ""
    echo "To view logs: tail -f $LOG_DIR/<service-name>.log"
    echo "To stop all services: ./run-services.sh stop"
    echo ""
    
    # Wait for services to be ready
    echo "Waiting for services to be ready..."
    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r module port <<< "$service_info"
        wait_for_service "$port" "$module"
    done
    
    echo ""
    echo -e "${GREEN}All services are ready!${NC}"
    echo "API Gateway: http://localhost:8080"
}

# Function to stop all services
stop_services() {
    echo -e "${YELLOW}Stopping all services...${NC}"
    
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${YELLOW}No PID file found. Services may not be running.${NC}"
        return 0
    fi
    
    # Read PIDs and kill processes
    while IFS=':' read -r pid module port; do
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "Stopping $module (PID: $pid)..."
            kill "$pid" 2>/dev/null || true
            # Wait a bit for graceful shutdown
            sleep 2
            # Force kill if still running
            if ps -p "$pid" > /dev/null 2>&1; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        fi
    done < "$PID_FILE"
    
    # Remove PID file
    rm -f "$PID_FILE"
    
    echo -e "${GREEN}All services stopped.${NC}"
}

# Function to check service status
status_services() {
    echo -e "${GREEN}Service Status:${NC}"
    echo ""
    
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${YELLOW}No services appear to be running.${NC}"
        return 0
    fi
    
    printf "%-25s %-10s %-10s %s\n" "SERVICE" "PORT" "PID" "STATUS"
    echo "----------------------------------------------------------------"
    
    while IFS=':' read -r pid module port; do
        if ps -p "$pid" > /dev/null 2>&1; then
            if check_port "$port"; then
                status="${GREEN}RUNNING${NC}"
            else
                status="${YELLOW}STARTING${NC}"
            fi
            printf "%-25s %-10s %-10s %s\n" "$module" "$port" "$pid" "$status"
        else
            printf "%-25s %-10s %-10s %s\n" "$module" "$port" "N/A" "${RED}STOPPED${NC}"
        fi
    done < "$PID_FILE"
}

# Function to show logs
show_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        echo "Available services:"
        for service_info in "${SERVICES[@]}"; do
            IFS=':' read -r module port <<< "$service_info"
            echo "  - $module"
        done
        echo ""
        echo "Usage: ./run-services.sh logs <service-name>"
        return 1
    fi
    
    local log_file="$LOG_DIR/$service.log"
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        echo -e "${RED}Log file not found: $log_file${NC}"
        return 1
    fi
}

# Function to build all services
build_all() {
    echo -e "${GREEN}Building all services...${NC}"
    cd "$SCRIPT_DIR"
    mvn clean install -DskipTests
}

# Main command handling
case "$1" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        ;;
    status)
        status_services
        ;;
    logs)
        show_logs "$2"
        ;;
    build)
        build_all
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|build}"
        echo ""
        echo "Commands:"
        echo "  start    - Start all microservices"
        echo "  stop     - Stop all microservices"
        echo "  restart  - Restart all microservices"
        echo "  status   - Show status of all services"
        echo "  logs     - Show logs for a specific service (e.g., ./run-services.sh logs api-gateway)"
        echo "  build    - Build all services"
        exit 1
        ;;
esac

