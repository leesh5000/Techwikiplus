# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Core Commands
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run specific test class
./gradlew test --tests "me.helloc.techwikiplus.user.domain.model.UserTest"

# Run tests with pattern matching
./gradlew test --tests "*UserTest"

# Code formatting and linting
./gradlew ktlintCheck    # Check code style violations
./gradlew ktlintFormat   # Auto-format code

# Run the application
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args="--spring.profiles.active=bulk-loader"

# Clean build
./gradlew clean build
```

### Docker Infrastructure
```bash
# Start local infrastructure (MySQL, Redis, MailHog)
docker-compose -f docker-compose.infra.yml up -d

# Stop infrastructure
docker-compose -f docker-compose.infra.yml down

# View logs
docker-compose -f docker-compose.infra.yml logs -f [service-name]
```

### Database Management
```bash
# Flyway migrations are automatically applied on startup
# Migration files: src/main/resources/db/migration/V*.sql
# Naming: V{major}.{minor}.{patch}__{description}.sql
```

### API Documentation
```bash
# Generate OpenAPI documentation (runs after tests)
./gradlew test

# Documentation will be copied to: src/main/resources/static/api-docs/openapi3.yml
# Swagger UI: http://localhost:9000/swagger-ui/index.html
```

## Architecture Overview

### Hexagonal Architecture (Ports and Adapters)

The codebase follows a strict hexagonal architecture pattern with clear separation of concerns:

```
src/main/kotlin/me/helloc/techwikiplus/
├── common/                     # Shared components across bounded contexts
│   ├── domain/                 # Core domain concepts
│   │   └── service/
│   │       └── port/          # Port interfaces (AuthorizationPort, ClockHolder)
│   ├── infrastructure/         # Technical implementations
│   │   ├── cache/             # Redis cache implementation
│   │   ├── clock/             # System clock implementation
│   │   ├── id/                # Snowflake ID generation
│   │   ├── lock/              # Redis distributed locks
│   │   ├── mail/              # Email service
│   │   ├── persistence/       # JPA entities and repositories
│   │   ├── security/          # JWT authentication, Spring Security
│   │   └── web/               # Web configuration, filters
│   └── interfaces/            # API layer
│       └── web/               # Common web components
│
├── post/                      # Post bounded context
│   ├── domain/
│   │   ├── model/            # Domain models (Post, PostHistory, Review, Tag)
│   │   ├── service/          # Domain services
│   │   └── service/port/     # Port interfaces for post context
│   └── interfaces/
│       ├── scheduler/        # Scheduled tasks
│       └── web/              # REST controllers and DTOs
│
└── user/                      # User bounded context
    ├── domain/
    │   ├── model/            # User domain model
    │   ├── service/          # User domain services
    │   └── service/port/     # Port interfaces for user context
    └── interfaces/
        └── web/              # User REST controllers
```

### Key Architectural Principles

1. **Domain Layer Independence**: Domain models and services have no dependencies on infrastructure or framework code
2. **Port-Adapter Pattern**: All external dependencies are accessed through port interfaces
3. **Bounded Contexts**: Clear separation between User and Post contexts
4. **ID Generation**: Uses Snowflake algorithm for distributed ID generation
5. **Event Sourcing**: PostHistory tracks all changes to posts
6. **Repository Pattern**: JPA entities are separate from domain models with explicit mapping

## Testing Strategy

### Test Types
- **Unit Tests**: Domain model and service logic testing
- **Integration Tests**: Repository and infrastructure component testing with TestContainers
- **E2E Tests**: Full API testing with `@E2eTest` annotation
- **Architecture Tests**: ArchUnit enforces architectural rules

### Test Configuration
- TestContainers automatically provisions MySQL and Redis for integration tests
- Kotest is available alongside JUnit5 for BDD-style testing
- MockK is used for Kotlin-specific mocking

## Security Configuration

- JWT-based authentication with access and refresh tokens
- Spring Security with custom JWT filter chain
- Authorization through `AuthorizationPort` interface
- Password encoding using BCrypt

## Database Schema

- Managed by Flyway migrations
- Soft deletes implemented for posts
- Version control for posts with optimistic locking
- Indexes optimized for pagination queries

## Performance Considerations

- Redis caching for frequently accessed data
- Distributed locks using Redis for concurrency control
- Batch processing configured for JPA (batch size: 5000)
- Connection pooling with HikariCP (max 30 connections)

## Environment Variables

Key environment variables (with defaults for local development):
- `SPRING_MYSQL_HOST` (localhost)
- `SPRING_MYSQL_PORT` (13306)
- `SPRING_REDIS_HOST` (localhost)
- `SPRING_REDIS_PORT` (16379)
- `SPRING_JWT_SECRET` (development default provided)

## Code Style

- Kotlin code style enforced by ktlint
- Git pre-commit hook available: `./gradlew addKtlintCheckGitPreCommitHook`
- No unnecessary comments in production code
- Comprehensive test coverage expected for new features