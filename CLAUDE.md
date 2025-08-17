# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Techwikiplus is a Spring Boot application built with Kotlin that provides user authentication and management services with JWT-based security, email verification, and Redis caching.

## Base Guidelines

- 모든 응답은 한글로 하세요.

## Build and Run Commands

### Build
```bash
./gradlew build                # Build the project
./gradlew clean build          # Clean and rebuild
./gradlew bootJar              # Build executable JAR
```

### Testing
```bash
./gradlew test                                                     # Run all tests
./gradlew test --tests "ClassName"                               # Run specific test class
./gradlew test --tests "ClassName.methodName"                    # Run specific test method
./gradlew test --tests "*E2eTest"                               # Run all E2E tests
./gradlew test -Pe2e                                            # Run tests tagged as E2E
```

### Code Quality
```bash
./gradlew ktlintCheck          # Check Kotlin code style
./gradlew ktlintFormat         # Auto-format Kotlin code
./gradlew addKtlintCheckGitPreCommitHook  # Add pre-commit hook for ktlint
```

### Running the Application
```bash
./gradlew bootRun              # Run Spring Boot application
docker-compose -f docker-compose.infra.yml up -d    # Start infrastructure services
docker-compose -f docker-compose.prod.yml up        # Run full production stack
```

## Architecture Overview

### Domain-Driven Design Structure

The codebase follows DDD principles with clear separation between layers:

```
src/main/kotlin/me/helloc/techwikiplus/
├── common/               # Shared infrastructure components
│   ├── infrastructure/   # Technical implementations
│   │   ├── cache/       # Redis cache implementation
│   │   ├── clock/       # Time abstraction
│   │   ├── id/          # Snowflake ID generation
│   │   ├── lock/        # Distributed locking
│   │   ├── mail/        # Email sending
│   │   ├── persistence/ # Database access
│   │   ├── security/    # JWT, authentication, authorization
│   │   └── web/         # Web configuration
│   └── interfaces/      # Common API interfaces
└── user/                # User bounded context
    ├── application/     # Use case implementations (Facades)
    ├── domain/          # Core business logic
    │   ├── exception/   # Domain exceptions
    │   ├── model/       # Domain entities and value objects
    │   └── service/     # Domain services and ports
    └── interfaces/      # API controllers and DTOs
```

### Key Architectural Patterns

1. **Hexagonal Architecture**: Domain logic is isolated from infrastructure through ports and adapters
   - Ports: Interfaces defined in `domain/service/port/`
   - Adapters: Implementations in `common/infrastructure/`

2. **Facade Pattern**: Application layer uses facades to orchestrate domain services
   - Each use case has a dedicated facade (e.g., `UserSignUpFacade`, `UserLoginFacade`)

3. **Repository Pattern**: Data access is abstracted through repository interfaces
   - Port: `UserRepository` interface
   - Implementation: `UserRepositoryImpl` with JPA

4. **Value Objects**: Domain modeling uses immutable value objects
   - `Email`, `Nickname`, `UserId`, `EncodedPassword`, etc.

### Security Architecture

- **JWT Authentication**: Stateless authentication using JWT tokens
  - Access tokens (1 hour validity)
  - Refresh tokens (30 days validity)
  - Secret key must be at least 256 bits (32 bytes)
  
- **Spring Security Integration**:
  - `JwtAuthenticationFilter`: Validates JWT tokens on each request
  - `CustomUserDetailsService`: Loads user details for authentication
  - `SecurityConfiguration`: Configures security rules and filters

### Testing Strategy

1. **Unit Tests**: Test domain logic in isolation using fake implementations
   - Located in `test/kotlin/.../domain/`
   - Use `Fake*` implementations for ports

2. **E2E Tests**: Full integration tests with real databases
   - Extend `BaseE2eTest` for proper setup
   - Use TestContainers for MySQL, Redis, and MailHog
   - Tagged with `@E2eTest` annotation

3. **Architecture Tests**: Enforce architectural rules
   - `ArchitectureTest.kt` uses ArchUnit

### Infrastructure Services

The application requires these services (provided via Docker):

- **MySQL**: Main database (port 3306 in container, 13306 on host)
- **Redis**: Caching and distributed locks (port 6379 in container, 16379 on host)
- **MailHog**: Email testing in development (SMTP: 1025/11025, Web: 8025/18025)
- **Prometheus & Grafana**: Monitoring (optional)

### Test Configuration

E2E tests use TestContainers with automatic configuration through `TestContainersInitializer`:
- Automatically starts MySQL, Redis, and MailHog containers
- Configures JWT secret for tests (minimum 32 bytes)
- Provides transaction rollback for test isolation
- Redis cache is cleared before each test

### Environment Variables

Key environment variables required:
- `SPRING_JWT_SECRET`: JWT signing key (minimum 32 bytes)
- `SPRING_MYSQL_*`: Database connection settings
- `SPRING_REDIS_*`: Redis connection settings
- `SPRING_MAIL_*`: Email server settings

### API Documentation

The project uses Spring REST Docs with OpenAPI integration:
- API documentation is generated during E2E tests
- OpenAPI spec available at: `/api-docs/openapi3.yml`
- Swagger UI available at: `/swagger-ui/index.html`

### Database Migration

Flyway manages database schema:
- Migrations in: `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`

## 코딩 규칙

### Kotlin 코딩 스타일
- `Enum.values()` 대신 `Enum.entries` 사용 (Kotlin 1.9+)
- Wildcard import 금지
- ktlint 규칙 준수 필수

### 테스트 코드 규칙
- **Kotest 프레임워크** 사용
- **FIRST 원칙** 준수 (Fast, Independent, Repeatable, Self-validating, Timely)
- **테스트 격리성** 보장
- 단위 테스트는 **Fake 객체** 사용 (예: `FakeUserRepository`, `FakeClockHolder`)
- 통합 테스트는 **TestContainers** 사용 (MySQL, Redis)
- E2E 테스트는 `BaseE2eTest` 상속하여 Spring REST Docs 자동 생성