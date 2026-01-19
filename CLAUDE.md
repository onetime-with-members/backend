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

## Documentation (IMPORTANT)

모든 기능 설계 및 문서화는 반드시 `docs/` 디렉터리에 작성해야 합니다.

### 문서 구조
```
docs/
├── design/              # 기능 설계 문서 (구현 전 설계 검토용)
│   └── *.md
├── features/            # 구현된 기능 명세 문서
│   └── *.md
└── sql/                 # DDL, 마이그레이션 스크립트
    └── *.sql
```

### 문서화 규칙
1. **새 기능 구현 전**: `docs/design/` 에 설계 문서 작성 후 검토
2. **기능 구현 완료 후**: `docs/features/` 에 기능 명세 이동 또는 작성
3. **DB 스키마 변경 시**: `docs/sql/` 에 DDL 스크립트 보관

## Test API (E2E Testing)

테스트 환경(local, dev)에서만 동작하는 테스트 전용 API가 있습니다.

### 테스트 로그인 API
- **Endpoint**: `POST /api/v1/test/auth/login`
- **Purpose**: Cypress E2E 테스트에서 소셜 로그인 없이 토큰 발급
- **Security**: `@ConditionalOnProperty`로 PROD에서 빈 자체 미생성
- **Config**: `test.auth.enabled=true` (local/dev만), `test.auth.secret-key` 환경변수 필요

설계 문서: `docs/design/test-login-api.md`
