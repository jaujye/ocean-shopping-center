# Multi-stage build for Ocean Shopping Center Backend
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build

# Install Maven wrapper dependencies
RUN apk add --no-cache dos2unix

# Set working directory
WORKDIR /workspace/app

# Copy Maven wrapper and make it executable
COPY mvnw .
COPY .mvn .mvn
RUN dos2unix mvnw && chmod +x mvnw

# Copy pom.xml and download dependencies (for better Docker layer caching)
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# Copy source code and build application
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Verify the built JAR file exists
RUN ls -la target/ && find target/ -name "*.jar" -type f

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install curl for health checks and tzdata for timezone support
RUN apk add --no-cache curl tzdata

# Create application user for security
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy the built JAR from build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Set proper ownership for the JAR file
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:G1HeapRegionSize=32m \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Default Spring profile for Docker
ENV SPRING_PROFILES_ACTIVE=docker

# Application metadata labels
LABEL maintainer="Ocean Shopping Center Team"
LABEL version="1.0.0"
LABEL description="Ocean Shopping Center Backend API"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]