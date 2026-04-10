#!/bin/bash

# Docker startup script for Trello Mini
# Usage: ./docker-start.sh

set -e

echo " Starting Trello Mini with Docker Compose..."

# Check if docker-compose exists
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo " Docker Compose is not installed"
    exit 1
fi

# Use docker compose instead of docker-compose if available
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Start services
$COMPOSE_CMD up -d

echo "Services started successfully!"
echo ""
echo " Service URLs:"
echo "  Application: http://localhost:3000"
echo "  MySQL: localhost:3306"
echo "  Redis: localhost:6379"
echo ""
echo " View logs:"
echo "  All:      $COMPOSE_CMD logs -f"
echo "  App:      $COMPOSE_CMD logs -f app"
echo "  MySQL:    $COMPOSE_CMD logs -f mysql"
echo "  Redis:    $COMPOSE_CMD logs -f redis"
echo ""
echo "To stop services, run: ./docker-stop.sh"
