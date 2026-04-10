#!/bin/bash
set -e

echo "⏳ Waiting for MySQL to be ready..."
while ! nc -z mysql 3306 2>/dev/null; do
  echo "  ⏳ MySQL is unavailable - sleeping"
  sleep 2
done
echo "✓ MySQL is ready!"

echo "⏳ Waiting for Redis to be ready..."
while ! nc -z redis 6379 2>/dev/null; do
  echo "  ⏳ Redis is unavailable - sleeping"
  sleep 2
done
echo "✓ Redis is ready!"

echo "🚀 Starting Trello Mini Application..."
exec java -Dspring.profiles.active=docker -jar /app/app.jar "$@"
