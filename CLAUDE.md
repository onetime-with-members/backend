# CLAUDE.md - OneTime Backend

This file provides guidance for Claude Code when working with this codebase.

## Project Overview

OneTime is a Spring Boot-based backend API for a collaborative event scheduling application. Users can create time-based events, participate in scheduling, and provide time availability. Supports both authenticated (OAuth2) and anonymous user participation.

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.2
- **Database**: MySQL 8.0 with Spring Data JPA, QueryDSL 5.0
- **Cache**: Redis with Redisson 3.46.0 (distributed locking)
- **Security**: Spring Security, OAuth2 (Google, Kakao, Naver), JWT (JJWT 0.12.2)
- **Cloud**: AWS S3 (Spring Cloud AWS 3.1.1), CodeDeploy
- **Documentation**: Spring REST Docs 3.0.0, SpringDoc OpenAPI 2.1.0
- **Build**: Gradle 8.x

## Common Commands

```bash
# Build
./gradlew clean build              # Full build with tests
./gradlew build -x test            # Build without tests

# Run
./gradlew bootRun --args='--spring.profiles.active=local'

# Test
./gradlew test                     # Run all tests

# Documentation
./gradlew openapi3                 # Generate OpenAPI spec
./gradlew asciidoctor              # Generate AsciiDoc docs

# Docker
docker build -t onetime-backend .
docker run -p 8090:8090 onetime-backend
```

## Project Structure

```
src/main/java/side/onetime/
├── controller/          # REST API endpoints (@RestController)
├── service/             # Business logic layer (@Service)
├── repository/          # Data access layer (JpaRepository, QueryDSL)
├── domain/              # JPA entities with Soft Delete pattern
│   └── enums/           # Status enums (Status, EventStatus, etc.)
├── dto/                 # DTOs organized by feature
│   └── <feature>/
│       ├── request/
│       └── response/
├── auth/                # OAuth2 & JWT authentication
├── global/
│   ├── config/          # Spring configurations
│   ├── filter/          # JwtFilter
│   ├── lock/            # @DistributedLock annotation & AOP
│   └── common/          # ApiResponse<T>, status codes, BaseEntity
├── exception/           # CustomException, GlobalExceptionHandler
├── infra/               # External integrations (Everytime client)
└── util/                # Utility classes (JwtUtil, S3Util, etc.)
```

## Code Conventions

### Architecture
- Layered architecture: Controller → Service → Repository → Domain
- RESTful API with `/api/v1/` prefix
- Generic response wrapper: `ApiResponse<T>` with `onSuccess()`, `onFailure()`

### Naming
- Controllers: `*Controller`
- Services: `*Service`
- Repositories: `*Repository`
- DTOs: `*Request`, `*Response` in feature-based packages
- Entities: PascalCase without suffix

### Patterns
- **Soft Delete**: `@SQLDelete`, `@SQLRestriction` with `Status` enum (ACTIVE, DELETED)
- **Distributed Locking**: `@DistributedLock` annotation for race condition prevention
- **DTO Conversion**: `toEntity()` methods, static factory `of()` methods
- **Error Handling**: Domain-specific error status enums (e.g., `EventErrorStatus`)
- **Dependency Injection**: Constructor injection with `@RequiredArgsConstructor`

### Database
- Hibernate with fetch join for N+1 prevention
- QueryDSL for complex queries with custom repository implementations
- `@Transactional` for transaction management

## Commit Convention

Format: `[type]: description (#issue-number)`

Types:
- `[feat]`: New feature
- `[fix]`: Bug fix
- `[refactor]`: Code refactoring
- `[docs]`: Documentation

Example: `[feat] : 가이드 확인 여부를 조회/저장/삭제한다 (#300)`

## Branch Strategy

- `main`: Production
- `develop`: Development integration (base for features)
- `release/v*`: Release candidates (e.g., `release/v1.2.3`)
- `feature/#<issue>/<name>`: Feature branches (e.g., `feature/#4/login`)
- `hotfix/<description>`: Emergency fixes

## Testing

- JUnit 5 with Spring Boot Test
- MockMvc for controller integration tests
- Spring REST Docs for API documentation generation
- Test config uses port 8091

## Key Configuration

- Main config: `application.yaml`
- Profiles: `local`, `dev`, `prod`
- Server port: 8090 (default)
- Swagger UI: `/swagger-ui.html`
