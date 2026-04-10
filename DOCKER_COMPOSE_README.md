# Docker Setup Guide for Trello Mini

## Prerequisites

- Docker Desktop installed ([Download](https://www.docker.com/products/docker-desktop))
- Docker Compose version 1.29+ (included with Docker Desktop)

## Quick Start

### Option 1: Using Helper Scripts (Recommended)

```bash
# Start all services
./docker-start.sh

# Stop all services
./docker-stop.sh
```

### Option 2: Using Docker Compose Directly

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Stop and remove volumes (database data)
docker-compose down -v

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f app      # Application
docker-compose logs -f mysql    # MySQL
docker-compose logs -f redis    # Redis
```

## Services

### Application (Trello Mini)
- **URL**: http://localhost:3000
- **Port**: 3000
- **Profile**: docker

### MySQL Database
- **Host**: localhost
- **Port**: 3306
- **Database**: trello
- **User**: root
- **Password**: 123456
- **Data**: Persisted in `mysql-data` volume

### Redis Cache
- **Host**: localhost
- **Port**: 6379
- **Data**: Persisted in `redis-data` volume

## Configuration

Environment variables are defined in `.env` file:

```env
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=trello
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_USER=root
REDIS_HOST=redis
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=3000
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION_MS=86400000
```

## Health Checks

Services have health checks configured:
- MySQL: Checks every 3 seconds, retries up to 10 times
- Redis: Checks every 3 seconds, retries up to 10 times
- Application: Starts only after MySQL and Redis are healthy

## Troubleshooting

### Port Already in Use

If you get "port already in use" error, change the port in `docker-compose.yml`:

```yaml
ports:
  - "3001:3000"  # Use 3001 instead of 3000
```

### Database Connection Error

Ensure MySQL has started and is healthy:
```bash
docker-compose ps
```

If MySQL is not healthy, check logs:
```bash
docker-compose logs mysql
```

### Clear Everything and Start Fresh

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove the built image
docker-compose rm -f

# Rebuild and start
docker-compose up -d --build
```

### Access Database from Host

```bash
# Using MySQL CLI
mysql -h localhost -P 3306 -u root -p123456 -D trello

# Using a GUI client (e.g., MySQL Workbench, DBeaver)
# Connection: localhost:3306, user: root, password: 123456
```

## Building the Application Image

The image is built automatically when running `docker-compose up`. To rebuild:

```bash
docker-compose build --no-cache
```

## Logs

```bash
# All logs
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f mysql
docker-compose logs -f redis

# Last 100 lines
docker-compose logs --tail=100

# Specific time period
docker-compose logs --since 10m   # Last 10 minutes
docker-compose logs --until 5m    # Until 5 minutes ago
```

## Deployment Notes

For production deployment:
1. Change `MYSQL_ROOT_PASSWORD` in `.env`
2. Update `JWT_SECRET` with a secure value
3. Set `spring.jpa.hibernate.ddl-auto=validate` (don't auto-create schema)
4. Use managed MySQL and Redis services (RDS, ElastiCache) instead of containers
5. Configure proper backup strategy for data volumes

## Network

All services communicate through a dedicated Docker network `trello-network`:
- Application → MySQL: `jdbc:mysql://mysql:3306/trello`
- Application → Redis: `redis://redis:6379`

From host machine:
- Application: `http://localhost:3000`
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
