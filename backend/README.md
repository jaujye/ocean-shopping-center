# Ocean Shopping Center Backend

Spring Boot backend API for the Ocean Shopping Center multi-tenant e-commerce platform.

## Architecture Overview

### Technology Stack
- **Java 17+** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Spring Data JPA** - Data persistence layer
- **PostgreSQL** - Primary database
- **Redis** - Session management and caching
- **WebSocket** - Real-time communication
- **OpenAPI/Swagger** - API documentation
- **Maven** - Build tool
- **JUnit 5** - Testing framework

### Project Structure

```
backend/
├── src/main/java/com/ocean/shopping/
│   ├── OceanShoppingCenterApplication.java    # Main application class
│   ├── config/                                # Configuration classes
│   │   ├── DatabaseConfig.java               # Database configuration
│   │   ├── RedisConfig.java                  # Redis configuration
│   │   ├── WebSocketConfig.java              # WebSocket configuration
│   │   └── OpenApiConfig.java                # API documentation configuration
│   ├── controller/                           # REST controllers
│   │   └── HealthController.java             # Health check endpoints
│   ├── service/                              # Business logic layer
│   ├── repository/                           # Data access layer
│   │   └── UserRepository.java               # User data repository
│   ├── model/entity/                         # JPA entities
│   │   ├── User.java                         # User entity
│   │   ├── Store.java                        # Store entity
│   │   ├── Product.java                      # Product entity
│   │   ├── Order.java                        # Order entity
│   │   └── ...                               # Additional entities
│   ├── exception/                            # Exception handling
│   │   ├── GlobalExceptionHandler.java       # Global error handler
│   │   ├── ErrorResponse.java                # Error response DTO
│   │   └── ...                               # Custom exceptions
│   └── websocket/                            # WebSocket handlers
│       └── ChatController.java               # Real-time chat controller
├── src/main/resources/
│   ├── application.yml                       # Main configuration
│   └── logback-spring.xml                    # Logging configuration
└── src/test/                                # Test classes
    ├── java/com/ocean/shopping/
    │   └── OceanShoppingCenterApplicationTests.java
    └── resources/
        └── application-test.yml              # Test configuration
```

## Features Implemented

### ✅ Core Infrastructure
- [x] Spring Boot project setup with Maven
- [x] Multi-profile configuration (development/production/test)
- [x] PostgreSQL database connectivity with connection pooling
- [x] JPA/Hibernate configuration with optimized settings
- [x] Redis integration for session management
- [x] WebSocket support for real-time features

### ✅ Data Layer
- [x] JPA entities matching existing database schema
- [x] Repository interfaces with custom queries
- [x] Proper entity relationships and constraints
- [x] Database naming strategy configuration

### ✅ Web Layer
- [x] Health check endpoints (`/health`, `/health/detailed`, `/health/ready`, `/health/live`)
- [x] Global exception handling with consistent error responses
- [x] OpenAPI/Swagger documentation setup
- [x] CORS configuration

### ✅ Real-time Features
- [x] WebSocket configuration with STOMP protocol
- [x] Basic chat controller for real-time messaging
- [x] Message broker setup for pub/sub messaging

### ✅ Observability
- [x] Structured logging with Logback
- [x] Health check endpoints for container orchestration
- [x] Actuator endpoints for monitoring

### ✅ Testing
- [x] Unit test structure with Spring Boot Test
- [x] H2 in-memory database for testing
- [x] Test-specific configuration profiles

## Configuration

### Database Configuration
The application uses PostgreSQL as the primary database. Configuration is handled through environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:ocean_shopping_center}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
```

### Redis Configuration
Redis is used for session management and caching:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Profile-Specific Settings
- **Development**: Enhanced logging, H2 console enabled for tests
- **Production**: Optimized logging, security settings enforced
- **Test**: In-memory H2 database, Redis disabled

## API Endpoints

### Health Check Endpoints
- `GET /api/health` - Basic application health status
- `GET /api/health/detailed` - Detailed health with dependency status
- `GET /api/health/ready` - Kubernetes readiness probe
- `GET /api/health/live` - Kubernetes liveness probe

### WebSocket Endpoints
- `WS /api/ws` - WebSocket connection endpoint
- Topics: `/topic/public`, `/queue/*`, `/user/*`

### API Documentation
- Swagger UI: `http://localhost:3000/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:3000/api/v3/api-docs`

## Development

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 13+
- Redis 6+

### Running the Application

1. **Start dependencies** (PostgreSQL and Redis):
   ```bash
   docker-compose up -d postgres redis
   ```

2. **Set environment variables**:
   ```bash
   export POSTGRES_HOST=localhost
   export POSTGRES_PORT=5432
   export POSTGRES_DB=ocean_shopping_center
   export POSTGRES_USER=postgres
   export POSTGRES_PASSWORD=postgres
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   ```

3. **Run the application**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. **Access the application**:
   - API Base: http://localhost:3000/api
   - Health Check: http://localhost:3000/api/health
   - Swagger UI: http://localhost:3000/api/swagger-ui.html

### Running Tests

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database and don't require external dependencies.

### Building

```bash
cd backend
mvn clean package
```

This creates a JAR file in `target/shopping-center-1.0.0.jar`.

## Database Schema

The application uses the existing database schema with the following key entities:

- **Users** - User accounts with role-based access (customer, store_owner, administrator)
- **Stores** - Multi-tenant store management
- **Products** - Product catalog with variants and images
- **Orders** - Order management with comprehensive tracking
- **Categories** - Hierarchical product categorization
- **Reviews** - Product reviews and ratings
- **Messages** - Customer support communication

All entities include proper audit fields (created_at, updated_at) and are optimized with indexes for performance.

## Security

### Authentication & Authorization
- JWT-based authentication support configured
- Role-based access control (RBAC) with User entity
- Spring Security integration prepared

### Session Management
- Redis-based session storage
- Configurable session timeout
- Secure cookie configuration

## Monitoring & Operations

### Health Checks
- Application health with dependency checking
- Database connectivity verification
- Redis connectivity verification
- Kubernetes-ready probe endpoints

### Logging
- Structured logging with correlation IDs
- Environment-specific log levels
- Log aggregation ready (JSON format in production)

### Metrics
- Actuator endpoints for application metrics
- JVM metrics and health indicators

## Next Steps

The foundation is now ready for implementing specific business features:

1. **Authentication System** - JWT-based auth with login/registration
2. **Store Management** - CRUD operations for stores
3. **Product Management** - Product catalog management
4. **Order Processing** - Order lifecycle management
5. **Payment Integration** - Payment gateway integration
6. **Search & Filtering** - Product search with Elasticsearch
7. **Notification System** - Email and real-time notifications

## Performance Considerations

- Connection pool optimization with HikariCP
- JPA batch processing for bulk operations
- Redis caching for frequently accessed data
- Database indexes for common queries
- Pagination support for large datasets

## Testing Strategy

- Unit tests for service layer logic
- Integration tests for data layer
- Web layer tests with MockMVC
- WebSocket tests for real-time features
- Performance tests for critical paths