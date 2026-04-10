#!/bin/bash

# Local startup script for Trello Mini (non-Docker)
# Usage: ./start-local.sh

set -e

# Load .env file if exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
    echo "Loaded .env file"
fi

# Override with local dev values
export JWT_SECRET="${JWT_SECRET:-TrelloMiniSecretKey1234567890TrelloMiniSecretKey1234567890TrelloMini}"
export JWT_EXPIRATION="${JWT_EXPIRATION:-86400000}"

# Check required Google OAuth2 credentials
if [ -z "$GOOGLE_CLIENT_ID" ] || [ -z "$GOOGLE_CLIENT_SECRET" ]; then
    echo "ERROR: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must be set"
    echo "Please set them in .env file or as environment variables"
    exit 1
fi

echo "Starting Trello Mini..."
echo "  Port: 8088"
echo "  Google Client ID: ${GOOGLE_CLIENT_ID:0:20}..."

JAR="target/Trello_mini-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found. Building..."
    mvn clean package -DskipTests -q
fi

nohup java -jar "$JAR" > app.log 2>&1 &
echo "Started PID $!"
echo "Logs: tail -f app.log"
