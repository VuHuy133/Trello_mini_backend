# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven configuration and files
COPY maven-settings.xml ./settings.xml
COPY pom.xml .
COPY .mvn .mvn

# Copy source code
COPY src ./src

# Build application with custom Maven settings and increased timeout
RUN mvn clean package -DskipTests -q -s ./settings.xml

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install netcat for health checks
RUN apk add --no-cache netcat-openbsd curl bash

# Copy built JAR from builder
COPY --from=builder /build/target/Trello_mini-0.0.1-SNAPSHOT.jar app.jar

# Copy entrypoint script first
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8088

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
