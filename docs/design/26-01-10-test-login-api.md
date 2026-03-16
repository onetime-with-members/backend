# 테스트 전용 로그인 API 설계 문서

## 1. 배경 및 요구사항

### 1.1 배경
프론트엔드에서 Cypress를 이용한 E2E 테스트 자동화 구축 중, 소셜 로그인(카카오/구글/네이버) 방식의 한계로 인해 테스트 환경에서 프로그래매틱 로그인이 필요함.

### 1.2 문제점
- 소셜 로그인 UI를 Cypress에서 제어하면 속도 저하 및 봇 탐지로 인한 차단 발생
- 온보딩 페이지는 최초 회원가입 시에만 진입 가능하나, 매번 신규 계정 생성 불가능
- 기존 방식: 만료 기간이 긴 토큰을 쿠키에 직접 삽입 → 불안정하고 온보딩 테스트 불가

### 1.3 요구사항
- **테스트 환경(local, dev)에서만 동작**하는 별도의 로그인 API 추가
- 시크릿 키를 보내면 소셜 인증 과정을 건너뛰고 즉시 Access/Refresh Token 응답
- **PROD 환경에서는 절대 동작하지 않도록** 보안 설정 필수
- **실제 테스트 DB의 유저(id=121)를 사용**하여 토큰 발급

---

## 2. 테스트 유저 정보

테스트 환경 DB에 미리 생성된 테스트 전용 유저:

| 필드 | 값 |
|------|------|
| id | 121 |
| email | test@test.com |
| name | 테스트유저 |
| nickname | 테스트유저 |
| provider | test |
| providerId | 12345678901234567890 |
| status | ACTIVE |

---

## 3. API 설계

### 3.1 엔드포인트
```
POST /api/v1/test/auth/login
```

### 3.2 요청 (Request)

#### Headers
```
Content-Type: application/json
```

#### Body
```json
{
  "secret_key": "<서버에서 정의한 시크릿 키>"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `secret_key` | String | O | 테스트 API 인증용 시크릿 키 (64자 hex) |

### 3.3 응답 (Response)

#### 성공 시 (201 Created)
온보딩 API(`POST /api/v1/users/onboarding`)와 동일한 응답 구조 사용

```json
{
  "is_success": true,
  "code": "201",
  "message": "테스트 로그인에 성공했습니다.",
  "payload": {
    "access_token": "eyJhbGciOiJIUzI1...",
    "refresh_token": "eyJhbGciOiJIUzI1..."
  }
}
```

#### 실패 시
| 상황 | HTTP Status | 에러 코드 | 메시지 |
|------|-------------|-----------|--------|
| 잘못된 시크릿 키 | 401 | TEST-001 | 유효하지 않은 테스트 시크릿 키입니다. |
| PROD 환경에서 호출 | 403 | TEST-002 | 프로덕션 환경에서는 테스트 API를 사용할 수 없습니다. |

---

## 4. 구현 설계

### 4.1 파일 구조

```
src/main/java/side/onetime/
├── controller/
│   └── TestAuthController.java          # 테스트 전용 컨트롤러
├── service/
│   └── TestAuthService.java             # 테스트 로그인 로직
├── dto/
│   └── test/
│       └── request/
│           └── TestLoginRequest.java    # 요청 DTO
├── exception/
│   └── status/
│       └── TestErrorStatus.java         # 테스트 API 에러 상태
└── global/
    └── common/
        └── status/
            └── SuccessStatus.java       # 기존 파일에 추가
```

### 4.2 설정 파일 변경

#### application.yaml (기본)
```yaml
test:
  auth:
    enabled: false  # 기본값: 비활성화
    secret-key: ${TEST_AUTH_SECRET_KEY:}
    user-id: 121    # 테스트 유저 ID
```

#### application-local.yaml
```yaml
test:
  auth:
    enabled: true
```

#### application-dev.yaml
```yaml
test:
  auth:
    enabled: true
```

#### application-prod.yaml
```yaml
test:
  auth:
    enabled: false  # 명시적 비활성화
```

### 4.3 핵심 컴포넌트 설계

#### TestAuthController.java
```java
@RestController
@RequestMapping("/api/v1/test/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestAuthController {

    private final TestAuthService testAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OnboardUserResponse>> testLogin(
            @Valid @RequestBody TestLoginRequest request) {

        OnboardUserResponse response = testAuthService.login(request);
        return ApiResponse.onSuccess(SuccessStatus._TEST_LOGIN, response);
    }
}
```

#### TestAuthService.java
```java
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestAuthService {

    @Value("${test.auth.secret-key}")
    private String testSecretKey;

    @Value("${test.auth.user-id}")
    private Long testUserId;

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public OnboardUserResponse login(TestLoginRequest request) {
        // 1. 시크릿 키 검증
        if (!testSecretKey.equals(request.secretKey())) {
            throw new CustomException(TestErrorStatus._INVALID_SECRET_KEY);
        }

        // 2. 고정된 테스트 유저 ID(121)로 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(testUserId, "USER");
        String browserId = jwtUtil.hashUserAgent("E2E-Test-Agent");
        String refreshToken = jwtUtil.generateRefreshToken(testUserId, browserId);

        // 3. Refresh Token Redis 저장
        RefreshToken token = RefreshToken.of(testUserId, browserId, refreshToken);
        refreshTokenRepository.save(token);

        return OnboardUserResponse.of(accessToken, refreshToken);
    }
}
```

#### TestLoginRequest.java
```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TestLoginRequest(
    @NotBlank(message = "시크릿 키는 필수입니다.")
    String secretKey
) {}
```

#### TestErrorStatus.java
```java
@Getter
@AllArgsConstructor
public enum TestErrorStatus implements BaseErrorCode {
    _INVALID_SECRET_KEY(HttpStatus.UNAUTHORIZED, "TEST-001", "유효하지 않은 테스트 시크릿 키입니다."),
    _TEST_API_DISABLED(HttpStatus.FORBIDDEN, "TEST-002", "프로덕션 환경에서는 테스트 API를 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() { ... }

    @Override
    public ReasonDto getReasonHttpStatus() { ... }
}
```

### 4.4 보안 설정 (SecurityConfig.java)

최근 Security 로직이 Role 기반 인가로 변경됨. 테스트 API는 인증 없이 접근해야 하므로 `PUBLIC_URLS`에 추가 필요.

```java
// PUBLIC_URLS 배열에 추가
private static final String[] PUBLIC_URLS = {
    "/",
    "/login/**",
    // ... 기존 URL들 ...
    "/api/v1/test/**"   // 테스트 API 추가
};
```

**현재 Security 구조**:
- `PUBLIC_URLS`: 인증 없이 접근 가능 (`.permitAll()`)
- `AUTHENTICATED_USER_URLS`: `ROLE_USER` 권한 필요 (`.hasRole("USER")`)
- `AUTHENTICATED_ADMIN_URLS`: `ROLE_ADMIN` 권한 필요 (`.hasRole("ADMIN")`)

**발급되는 토큰의 권한**:
- `userType: "USER"` → JwtFilter에서 `CustomUserDetails` 로드 → `ROLE_USER` 권한 부여
- 따라서 발급된 토큰으로 `/api/v1/users/**`, `/api/v1/fixed-schedules/**` 접근 가능

`@ConditionalOnProperty`로 인해 PROD에서는 컨트롤러 자체가 빈으로 등록되지 않으므로, PROD에서는 404 응답.

---

## 5. 보안 고려사항

### 5.1 다중 방어 레이어

| 레이어 | 방어 수단 | 설명 |
|--------|-----------|------|
| 1 | `@ConditionalOnProperty` | PROD에서 빈 자체가 생성되지 않음 (Controller, Service 모두) |
| 2 | `test.auth.enabled` 설정 | application-prod.yaml에서 명시적 false |
| 3 | 시크릿 키 검증 | 환경변수로 관리되는 64자 해시값 필요 |

### 5.2 시크릿 키 생성

```bash
# 64자 랜덤 해시 생성
openssl rand -hex 32
# 예: a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456
```

### 5.3 환경변수 설정

```bash
# local/dev 환경에만 설정
TEST_AUTH_SECRET_KEY=<생성된 64자 해시>
```

---

## 6. 테스트 시나리오

### 6.1 Cypress 사용 예시

```javascript
// cypress/support/commands.js
Cypress.Commands.add('testLogin', () => {
  cy.request({
    method: 'POST',
    url: '/api/v1/test/auth/login',
    body: {
      secret_key: Cypress.env('TEST_AUTH_SECRET_KEY')
    }
  }).then((response) => {
    expect(response.status).to.eq(201);
    const { access_token, refresh_token } = response.body.payload;

    // 토큰을 로컬 스토리지 또는 쿠키에 저장
    window.localStorage.setItem('accessToken', access_token);
    window.localStorage.setItem('refreshToken', refresh_token);
  });
});

// 테스트에서 사용
describe('로그인 후 시나리오 테스트', () => {
  beforeEach(() => {
    cy.testLogin();
  });

  it('로그인된 상태에서 대시보드 접근', () => {
    cy.visit('/dashboard');
    // 테스트 로직...
  });
});
```

### 6.2 cypress.env.json 예시
```json
{
  "TEST_AUTH_SECRET_KEY": "<64자 해시>"
}
```

---

## 7. 장점

### 7.1 실제 DB 유저 사용의 이점
- 발급된 토큰으로 **모든 API 정상 테스트 가능**
- 유저 프로필 조회, 스케줄 생성 등 DB 의존 API도 문제없이 동작
- 별도 목킹 불필요

### 7.2 기존 대비 개선점
- 만료 기간이 긴 토큰 수동 삽입 → **API 호출로 즉시 신규 토큰 발급**
- 토큰 만료 걱정 없음
- 일관된 테스트 환경 보장

---

## 8. 참고: 기존 인증 플로우 비교

### 8.1 일반 소셜 로그인 플로우
```
1. 클라이언트 → OAuth2 Provider (Google/Kakao/Naver)
2. OAuth2 Provider → OAuthLoginSuccessHandler
3. 신규 유저: Register Token 발급 → 온보딩 API 호출 → Access/Refresh Token
4. 기존 유저: Access/Refresh Token 직접 발급
```

### 8.2 테스트 로그인 플로우 (신규)
```
1. Cypress → POST /api/v1/test/auth/login (secret_key)
2. 시크릿 키 검증
3. 테스트 유저 ID(121)로 토큰 생성
4. Access/Refresh Token 직접 발급
5. Refresh Token Redis 저장
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2026-01-04 | Claude | 초안 작성 |
| 1.1 | 2026-01-09 | Claude | 실제 DB 테스트 유저(id=121) 사용으로 변경, email 파라미터 제거 |
| 1.2 | 2026-01-10 | Claude | Security Role 기반 인가 변경 반영 (hasRole("USER")) |
