# Trello Mini - Quick Start Guide

## 🚀 Fast Start (with Docker Compose)

### Prerequisites
- Docker & Docker Compose installed
- Git (optional, for cloning)

### Steps

1. **Navigate to project directory**
```bash
cd Trello_mini
```

2. **Start the applications**
```bash
docker-compose up -d
```

3. **Wait for services to start** (about 30 seconds)
```bash
docker-compose logs -f
```

4. **Access the application**
- Backend API: http://localhost:3000
- MySQL: localhost:3306
- Redis: localhost:6379

### Sample API Calls

#### Register a User
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### Login
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### Create a Project (Replace JWT_TOKEN with token from login)
```bash
curl -X POST http://localhost:3000/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d '{
    "name": "My First Project",
    "description": "Project description"
  }'
```

## 📋 Using Postman

1. Open Postman
2. Import `Trello_Mini_API.postman_collection.json`
3. Set the `jwt_token` variable after login
4. Use the pre-configured requests

## 🛑 Stop the Application

```bash
docker-compose down
```

For volumes cleanup:
```bash
docker-compose down -v
```

## 📚 Full Documentation

See [README.md](README.md) for complete documentation.

## ⚙️ Local Development Setup

If running locally without Docker:

1. **Install prerequisites**
   - JDK 17+
   - MySQL 8.0+
   - Redis 7+

2. **Configure application**
   - Edit `src/main/resources/application.properties`
   - Update database credentials

3. **Build and run**
```bash
mvn clean package -DskipTests
java -jar target/Trello_mini-0.0.1-SNAPSHOT.jar
```

## 🐛 Troubleshooting

### Port 3000 already in use
```bash
# Change port in application.properties or docker-compose.yml
# Then restart
```

### MySQL connection refused
```bash
# Ensure MySQL is running
docker-compose logs mysql
```

### Redis connection error
```bash
# Check Redis is running
docker-compose logs redis
```

## 📝 Default Credentials

- **MySQL**
  - User: root
  - Password: 123456
  - Database: trello

- **Redis**
  - Default (no authentication)

## 🔐 Security Notes

- Change JWT secret in production (.env file)
- Change MySQL credentials
- Enable HTTPS
- Set proper CORS origins
