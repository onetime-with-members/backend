# CLAUDE.md - OneTime Backend

This file provides guidance for Claude Code when working with this codebase.

## Project Overview

OneTime is a Spring Boot-based backend API for a collaborative event scheduling application. Users can create time-based events, participate in scheduling, and provide time availability. Supports both authenticated (OAuth2) and anonymous user participation.

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.2
- **Database**: MySQL 8.0 with Spring Data JPA, QueryDSL 5.0
- **Security**: Spring Security, OAuth2 (Google, Kakao, Naver), JWT (JJWT 0.12.2)
- **Cloud**: AWS S3 (Spring Cloud AWS 3.1.1), SQS (AWS SDK 2.25.30), CodeDeploy
- **Email**: AWS SQS (비동기 큐) → Batch → AWS SES (발송). 상세: `docs/features/26-01-26-admin-statistics.md`
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
├── service/             # Business logic + EmailEventPublisher (SQS)
├── repository/          # Data access layer (JpaRepository, QueryDSL)
├── domain/              # JPA entities with Soft Delete pattern
│   └── enums/           # Status enums (Status, EmailLogStatus, etc.)
├── dto/                 # DTOs organized by feature
│   └── <feature>/
│       ├── request/     # (EmailEventMessage = SQS 메시지 DTO)
│       └── response/
├── auth/
│   ├── annotation/      # @IsAdmin, @IsMasterAdmin, @IsUser, @PublicApi
│   └── service/         # OAuth2 & JWT authentication
├── global/
│   ├── config/          # Spring configs (SqsConfig, SecurityConfig, etc.)
│   ├── filter/          # JwtFilter
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
- **DTO Conversion**: `toEntity()` methods, static factory `of()` methods
- **Error Handling**: Domain-specific error status enums (e.g., `EventErrorStatus`)
- **Dependency Injection**: Constructor injection with `@RequiredArgsConstructor`
- **Security Annotations**: `@IsAdmin`, `@IsMasterAdmin`, `@IsUser`, `@PublicApi` (모든 API 필수, `SecurityAnnotationTest`로 검증)
- **Admin Auth**: BCrypt 비밀번호 해싱 (`PasswordEncoder`), JWT HttpOnly 쿠키 (항상 Secure)
- **Async Email**: Backend(SQS 발행) → Batch(SQS 소비 + SES 발송). `EmailEventPublisher` → `EmailEventMessage` DTO
- **Email Template Code**: `EmailTemplateCode` enum으로 API 트리거 이메일 템플릿 코드 관리
- **Banner Staging**: `BannerStaging`/`BarBannerStaging` 엔티티로 테스트↔운영 배너 동기화 (내보내기/불러오기)
- **Event Status**: ACTIVE, CONFIRMED, DELETED. 통계 쿼리는 `status != 'DELETED'`로 CONFIRMED 포함

### Database
- Hibernate with fetch join for N+1 prevention
- QueryDSL for complex queries with custom repository implementations
- `@Transactional` for transaction management

## Commit Convention

Format: `type: description`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `chore`: Maintenance tasks

Example: `feat: 만료된 액세스 토큰 발급 테스트 API를 추가한다`

### 커밋 제외 파일
- `docs/sql/` 디렉터리 (DDL 스크립트)
- OpenAPI 관련 생성 파일

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
- `SecurityAnnotationTest`: 클래스패스 스캔으로 모든 API에 권한 어노테이션 적용 여부 검증 (Spring 컨텍스트 미사용)
- Test config uses port 8091

### Swagger 예외 케이스 문서화

실패 케이스도 Swagger에 표시하려면 테스트에 `MockMvcRestDocumentationWrapper.document()` 추가:

```java
@Test
@DisplayName("[FAILED] 실패 케이스 설명")
public void someFailCase() throws Exception {
    // ... 테스트 로직 ...

    resultActions
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ERROR-001"))
            .andDo(MockMvcRestDocumentationWrapper.document("api/endpoint-fail-reason",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    resource(
                            ResourceSnippetParameters.builder()
                                    .tag("API Tag")
                                    .build()
                    )
            ));
}
```

- DisplayName에 `[FAILED]` prefix 추가
- document 경로에 `-fail-reason` suffix 추가

## Key Configuration

- Main config: `application.yaml`
- Profiles: `local`, `dev`, `prod`
- Server port: 8090 (default)
- Swagger UI: `/swagger-ui.html`
- SQS Queue URL: `${AWS_SQS_EMAIL_QUEUE_URL}` (이메일 비동기 큐)

## Documentation (IMPORTANT)

모든 기능 설계 및 문서화는 반드시 `docs/` 디렉터리에 작성해야 합니다.

### 문서 구조
```
docs/
├── ARCHITECTURE.md      # 아키텍처 & 기술 선정 문서
├── design/              # 기능 설계 문서 (구현 전 설계 검토용)
├── features/            # 구현된 기능 명세 문서
├── plans/               # 구현 계획 문서
├── security/            # 보안 관련 문서
└── sql/                 # DDL, 마이그레이션 스크립트 (gitignore)
```

### 문서화 규칙
1. **새 기능 구현 전**: `docs/design/` 에 설계 문서 작성 후 검토
2. **기능 구현 완료 후**: `docs/features/` 에 기능 명세 이동 또는 작성
3. **DB 스키마 변경 시**: `docs/sql/` 에 DDL 스크립트 보관

### docs 파일명 컨벤션

`yy-mm-dd-{설명}.md` — 예: `26-03-03-system-architecture.md`
- 설명은 다른 문서와 구분될 정도로 구체적으로 작명
- `docs/plans/` 하위도 동일 컨벤션 적용
- 단, `docs/ARCHITECTURE.md`는 제외하며 업데이트 시에도 네이밍을 그대로 유지

## Test API (E2E Testing)

테스트 환경(local, dev)에서만 동작하는 테스트 전용 API가 있습니다.

### 테스트 로그인 API
- **Endpoint**: `POST /api/v1/test/auth/login`
- **Purpose**: Cypress E2E 테스트에서 소셜 로그인 없이 토큰 발급
- **Security**: `@ConditionalOnProperty`로 PROD에서 빈 자체 미생성
- **Config**: `test.auth.enabled=true` (local/dev만), `test.auth.secret-key` 환경변수 필요

설계 문서: `docs/design/26-01-10-test-login-api.md`
