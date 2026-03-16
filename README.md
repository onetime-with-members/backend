# OneTime Backend

![디스콰이엇1](https://github.com/user-attachments/assets/1f07d38a-643f-4657-8623-bf36e81fbd77)

> 일정 조율 서비스 OneTime의 백엔드 API 서버

## System Architecture

<img width="983" alt="250529_아키텍처" src="https://github.com/user-attachments/assets/9c95e15d-e763-40ba-ab46-d1eab5c9acb7" />

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Database | MySQL 8.0, Spring Data JPA, QueryDSL 5.0 |
| Security | Spring Security, OAuth2 (Google/Kakao/Naver), JWT (JJWT 0.12.2) |
| Cloud | AWS S3, SQS, CodeDeploy, ECR |
| Email | AWS SQS (비동기 큐) → Batch → AWS SES (발송) |
| Documentation | Spring REST Docs 3.0.0, SpringDoc OpenAPI 2.1.0 |
| Admin | Thymeleaf, Tailwind CSS, Chart.js |
| Build | Gradle 8.x |
| CI/CD | GitHub Actions → Docker (ECR) → AWS SSM |

## Key Features

### Event Scheduling
- 날짜(DATE) / 요일(DAY) 기반 이벤트 생성
- 회원(OAuth2) + 비회원(PIN) 이중 참여 모델
- 스케줄 배치 INSERT (JdbcTemplate)
- 이벤트 확정 (ACTIVE → CONFIRMED)

### Authentication & Security
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- JWT Access Token + MySQL 기반 Refresh Token Rotation
- Grace Period (3초) 동시 요청 허용, 탈취 시 Family 전체 Revoke
- Admin BCrypt 비밀번호 해싱, HttpOnly Secure 쿠키

### Admin Dashboard
- 유저/이벤트 통계 대시보드 (KPI, 차트, 히트맵)
- 리텐션 분석 (MAU, 퍼널, TTV, Stickiness, 코호트)
- 이벤트 확정 통계 (확정률, 확정자 유형, 카테고리별 확정률)
- 마케팅 타겟 관리 (그룹별 유저 선택 + 이메일 발송)
- 이메일 템플릿 CRUD + SQS 비동기 발송
- 배너/띠배너 관리 (스테이징 내보내기/불러오기)
- 반응형 디자인 + 다크 모드 + PWA 지원

## Branch Strategy

| 브랜치 | 설명 |
|--------|------|
| `main` | 프로덕션 (PR 병합 시 prod-cicd 실행) |
| `release/v*` | 스테이징 검수 |
| `develop` | 개발 통합 (PR 병합 시 test-cicd 실행) |
| `feature/#<issue>/<name>` | 기능 개발 |
| `hotfix/<description>` | 긴급 수정 |

## Getting Started

```bash
# Build
./gradlew clean build

# Run (local)
./gradlew bootRun --args='--spring.profiles.active=local'

# Test
./gradlew test

# Docker
docker build -t onetime-backend .
docker run -p 8090:8090 onetime-backend
```

## API Documentation

- Swagger UI: `/swagger-ui.html`
- Admin Dashboard: `/admin/dashboard`

## Project Structure

```
src/main/java/side/onetime/
├── controller/          # REST API endpoints
├── service/             # Business logic
├── repository/          # Data access (JPA, QueryDSL)
├── domain/              # JPA entities
│   └── enums/           # Status enums
├── dto/                 # DTOs by feature
├── auth/                # OAuth2, JWT, Security annotations
├── global/              # Config, Filter, Common
├── exception/           # Error handling
└── util/                # Utilities
```

## Documentation

상세 설계 및 기능 명세는 `docs/` 디렉터리를 참고하세요.

```
docs/
├── ARCHITECTURE.md      # 아키텍처 & 기술 선정 이유
├── design/              # 기능 설계 문서
├── features/            # 구현된 기능 명세
├── plans/               # 구현 계획
└── security/            # 보안 관련 문서
```
