# Trello Mini - Project Management System

A mini Trello-like project management system built with Spring Boot, Spring Security, JWT, MySQL, and Redis.

## Features

- **User Authentication**: JWT-based authentication with login and registration
- **Project Management**: Create, read, update, and delete projects
- **Task Management**: Create and manage tasks within projects with priority and status tracking
- **Task Comments**: Add comments to tasks for collaboration
- **Project Members**: Manage project team members with roles
- **Task Attachments**: Upload and manage task attachments
- **Caching**: Redis integration for improved performance
- **Docker Support**: Full Docker and Docker Compose setup

## Technology Stack

- **Backend**: Java Spring Boot 4.0.3
- **JDK**: Java 17
- **Database**: MySQL 8.0
- **Cache**: Redis 7-alpine
- **Authentication**: Spring Security + JWT (jjwt 0.12.3)
- **ORM**: Spring Data JPA with Hibernate 7.2.4
- **Build Tool**: Maven 3.9.6
- **Code Simplification**: Lombok 1.18.42
- **Containerization**: Docker + Docker Compose

## Project Structure

```
src/main/java/com/trello/
├── config/           # Configuration classes
├── controller/       # REST API controllers
├── dto/              # Data Transfer Objects
├── entity/           # JPA entities
├── mapper/           # DTO mappers
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic services
└── security/         # JWT token provider
```

## Database Schema

### Entities

1. **User**: Authentication and user information
   - id, username, email, password, role, createdAt

2. **Project**: Project information
   - id, name, description, ownerId, createdAt, updatedAt

3. **ProjectMember**: Project team members
   - id, projectId, userId, role, joinedAt

4. **Task**: Project tasks
   - id, projectId, title, description, priority, status, assigneeId, dueDate, createdAt, updatedAt

5. **TaskComment**: Task comments
   - id, taskId, userId, content, createdAt

6. **TaskAttachment**: Task attachments/files
   - id, taskId, filename, uploadedById, createdAt

## Prerequisites

- Docker and Docker Compose installed
- OR
- JDK 17+
- MySQL 8.0+
- Redis 7+

## Getting Started

### Option 1: Using Docker Compose (Recommended)

1. Navigate to the project directory:
```bash
cd Trello_mini
```

2. Start the application with Docker Compose:
```bash
docker-compose up -d
```

The application will be available at `http://localhost:3000`

MySQL will run on `localhost:3306` with credentials:
- Username: `root`
- Password: `123456`
- Database: `trello`

Redis will run on `localhost:6379`

### Option 2: Running Locally

1. Ensure MySQL and Redis are running

2. Update `application.properties` with your MySQL and Redis connection details:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/trello
spring.datasource.username=root
spring.datasource.password=123456
spring.redis.host=localhost
spring.redis.port=6379
```

3. Build the project:
```bash
mvn clean package -DskipTests
```

4. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:3000`

## API Endpoints

### Authentication
- **POST** `/api/auth/register` - Register a new user
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

- **POST** `/api/auth/login` - Login user
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

- **POST** `/api/auth/logout` - Logout user (requires JWT token)

### Projects
- **POST** `/api/projects` - Create a new project
- **GET** `/api/projects` - Get all projects
- **GET** `/api/projects/{id}` - Get project by ID
- **PUT** `/api/projects/{id}` - Update project
- **DELETE** `/api/projects/{id}` - Delete project

### Tasks
- **POST** `/api/tasks` - Create a new task
- **GET** `/api/projects/{projectId}/tasks` - Get all tasks in a project
- **PUT** `/api/tasks/{id}` - Update task
- **DELETE** `/api/tasks/{id}` - Delete task

### Task Comments
- **POST** `/api/comments` - Add a comment to a task
- **GET** `/api/tasks/{taskId}/comments` - Get all comments for a task

### Project Members
- **POST** `/api/projects/{projectId}/members` - Add member to project
- **GET** `/api/projects/{projectId}/members` - Get project members

## Testing with Postman

A Postman collection is included: `Trello_Mini_API.postman_collection.json`

1. Import the collection in Postman
2. Set the environment variable `jwt_token` after login
3. Use the provided requests to test all endpoints

## Configuration Profiles

The application supports multiple configuration profiles:

- **application.properties** - Default configuration (MySQL localhost)
- **application-dev.properties** - Development configuration
- **application-docker.properties** - Docker environment configuration
- **application-prod.properties** - Production configuration (uses environment variables)

To use a specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=docker"
```

## Building Docker Image

Build the Docker image:
```bash
docker build -t trello-mini .
```

Run the container:
```bash
docker run -p 3000:3000 --network trello-network \
  -e SPRING_PROFILES_ACTIVE=docker \
  trello-mini
```

## Project Setup Details

### Spring Security Configuration
- JWT token validation on all protected endpoints
- CORS configuration for frontend integration
- Password encryption using BCryptPasswordEncoder

### Database Initialization
- Automatic schema creation on application startup (via Hibernate)
- Data relationships properly defined with foreign keys

### Caching Strategy
- Redis used for session caching
- Manual cache invalidation on data updates

## Known Issues & Troubleshooting

**Database Connection Error**
- Ensure MySQL is running and accessible
- Check credentials in configuration file
- Verify database name is correct (default: `trello`)

**Redis Connection Error**
- Ensure Redis is running on port 6379
- Check Redis connectivity: `redis-cli ping`

**Port Already in Use**
- Change port in `application.properties`: `server.port=8080`
- Or stop the service using the port

## Future Enhancements

- [ ] Real-time notifications using WebSocket
- [ ] Email notifications for assigned tasks
- [ ] Task filtering and search functionality
- [ ] User profile management
- [ ] Activity logging and audit trail
- [ ] Project templates
- [ ] Bulk operations

## License

MIT License - Feel free to use this project for learning purposes.

## Support

For issues or questions, please create an issue in the repository or contact the development team.
