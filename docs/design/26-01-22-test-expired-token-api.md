# 만료된 토큰 발급 테스트 API 설계 문서

## 1. 배경 및 요구사항

### 1.1 배경
프론트엔드 E2E 테스트에서 토큰 만료 시나리오(401 처리, 리프레시 토큰 재발급 플로우 등)를 테스트하기 위해 만료된 액세스 토큰이 필요함.

### 1.2 요구사항
- **테스트 환경(local, dev)에서만 동작**
- 시크릿 키 인증 후 **이미 만료된 액세스 토큰** 반환
- 기존 `TestAuthController` 패턴 재사용
- 응답은 test 도메인 전용 DTO 사용

---

## 2. API 설계

### 2.1 엔드포인트
```
POST /api/v1/test/auth/expired-token
```

### 2.2 요청 (Request)

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

### 2.3 응답 (Response)

#### 성공 시 (201 Created)
```json
{
  "is_success": true,
  "code": "201",
  "message": "만료된 테스트 토큰이 발급되었습니다.",
  "payload": {
    "access_token": "eyJhbGciOiJIUzI1..."
  }
}
```

#### 실패 시
| 상황 | HTTP Status | 에러 코드 | 메시지 |
|------|-------------|-----------|--------|
| 잘못된 시크릿 키 | 401 | TEST-001 | 유효하지 않은 테스트 시크릿 키입니다. |

---

## 3. 구현 설계

### 3.1 파일 구조 (변경/추가)

```
src/main/java/side/onetime/
├── controller/
│   └── TestAuthController.java          # 엔드포인트 추가
├── service/
│   └── TestAuthService.java             # 만료 토큰 생성 로직 추가
├── dto/
│   └── test/
│       ├── request/
│       │   └── TestLoginRequest.java    # 기존 재사용
│       └── response/
│           └── TestTokenResponse.java   # 신규 생성
└── util/
    └── JwtUtil.java                     # 만료 토큰 생성 메서드 추가
```

### 3.2 신규 DTO

#### TestTokenResponse.java
```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TestTokenResponse(
    String accessToken,
    String refreshToken
) {
    public static TestTokenResponse of(String accessToken, String refreshToken) {
        return new TestTokenResponse(accessToken, refreshToken);
    }

    // 액세스 토큰만 반환할 때 사용
    public static TestTokenResponse ofAccessToken(String accessToken) {
        return new TestTokenResponse(accessToken, null);
    }
}
```

**참고**: 기존 테스트 로그인 API(`/login`)도 `OnboardUserResponse` 대신 `TestTokenResponse`를 사용하도록 변경합니다.

### 3.3 JwtUtil 변경

```java
// 기존 generateAccessToken에 만료 시간 파라미터 추가 오버로드
public String generateAccessToken(Long userId, String userType, long expirationMs) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("userType", userType)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
}

// 만료된 토큰 생성 (1초 전 만료)
public String generateExpiredAccessToken(Long userId, String userType) {
    return generateAccessToken(userId, userType, -1000L); // 1초 전 만료
}
```

### 3.4 TestAuthController 변경

```java
@PostMapping("/expired-token")
public ResponseEntity<ApiResponse<TestTokenResponse>> getExpiredToken(
        @Valid @RequestBody TestLoginRequest request) {

    TestTokenResponse response = testAuthService.generateExpiredToken(request);
    return ApiResponse.onSuccess(SuccessStatus._TEST_EXPIRED_TOKEN, response);
}
```

### 3.5 TestAuthService 변경

```java
public TestTokenResponse generateExpiredToken(TestLoginRequest request) {
    // 1. 시크릿 키 검증
    if (!testSecretKey.equals(request.secretKey())) {
        throw new CustomException(TestErrorStatus._INVALID_SECRET_KEY);
    }

    // 2. 만료된 액세스 토큰 생성
    String expiredToken = jwtUtil.generateExpiredAccessToken(testUserId, "USER");

    return TestTokenResponse.of(expiredToken);
}
```

### 3.6 SuccessStatus 추가

```java
_TEST_EXPIRED_TOKEN(HttpStatus.CREATED, "201", "만료된 테스트 토큰이 발급되었습니다.")
```

---

## 4. 보안 고려사항

기존 테스트 로그인 API와 동일한 보안 레이어 적용:

| 레이어 | 방어 수단 |
|--------|-----------|
| 1 | `@ConditionalOnProperty` - PROD에서 빈 미생성 |
| 2 | `test.auth.enabled` 설정값 |
| 3 | 시크릿 키 검증 |

---

## 5. 테스트 시나리오

### 5.1 Cypress 사용 예시

```javascript
Cypress.Commands.add('getExpiredToken', () => {
  cy.request({
    method: 'POST',
    url: '/api/v1/test/auth/expired-token',
    body: {
      secret_key: Cypress.env('TEST_AUTH_SECRET_KEY')
    }
  }).then((response) => {
    expect(response.status).to.eq(201);
    return response.body.payload.access_token;
  });
});

// 401 처리 테스트
describe('토큰 만료 시나리오', () => {
  it('만료된 토큰으로 API 호출 시 401 응답', () => {
    cy.getExpiredToken().then((expiredToken) => {
      cy.request({
        method: 'GET',
        url: '/api/v1/users/profile',
        headers: { Authorization: `Bearer ${expiredToken}` },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(401);
      });
    });
  });
});
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2026-01-22 | Claude | 초안 작성 |
