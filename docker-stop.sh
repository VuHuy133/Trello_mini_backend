#!/bin/bash

# Docker stop script for Trello Mini
# Usage: ./docker-stop.sh

set -e

echo " Stopping Trello Mini services..."

# Use docker compose instead of docker-compose if available
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Stop services
$COMPOSE_CMD down

echo " Services stopped successfully!"
echo ""
echo " To remove volumes (database data) as well:"
echo "   $COMPOSE_CMD down -v"
