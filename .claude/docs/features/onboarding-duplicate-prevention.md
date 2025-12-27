# 온보딩 API 중복 호출 방어 로직

## 1. 현재 문제점

### 1.1 에러 로그 분석
```json
{
  "@timestamp": "2025-12-19T22:05:30",
  "message": "Duplicate entry '100119476322276872610' for key 'users.UK6jdo1l976be85wv43w6x6e6x2'",
  "level": "ERROR"
}
```

### 1.2 문제 상황
- **API**: `POST /api/v1/users/onboarding`
- **원인**: 프론트엔드에서 온보딩 API를 빠르게 중복 호출 (버튼 더블클릭, 네트워크 재시도 등)
- **결과**: `provider_id` unique constraint 위반으로 500 에러 발생
- **발생 패턴**: 같은 유저가 1초 내 2회 호출

### 1.3 현재 코드의 한계
```java
// UserService.java - 현재 코드
@Transactional
public OnboardUserResponse onboardUser(OnboardUserRequest request) {
    String registerToken = request.registerToken();
    jwtUtil.validateToken(registerToken);
    User newUser = createUserFromRegisterToken(request, registerToken);
    userRepository.save(newUser);  // 중복 시 DB 레벨에서 에러 발생
    // ...
}
```

- 저장 전 중복 체크 로직 없음
- DB unique constraint에만 의존하여 500 에러 반환
- 클라이언트가 적절한 에러 메시지를 받지 못함

---

## 2. 구현 완료 사항

### 2.1 방어 로직 추가
저장 전 `provider_id`로 기존 유저 존재 여부를 확인하고, 이미 가입된 경우 409 Conflict 반환.

```java
// UserService.java - 수정 코드
@Transactional
public OnboardUserResponse onboardUser(OnboardUserRequest request) {
    String registerToken = request.registerToken();
    jwtUtil.validateToken(registerToken);

    // 중복 가입 방어 로직
    String providerId = jwtUtil.getClaimFromToken(registerToken, "providerId", String.class);
    if (userRepository.existsByProviderId(providerId)) {
        throw new CustomException(UserErrorStatus._ALREADY_REGISTERED_USER);
    }

    User newUser = createUserFromRegisterToken(request, registerToken);
    userRepository.save(newUser);
    // ...
}
```

### 2.2 에러 코드 추가
```java
// UserErrorStatus.java
_ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "USER-007", "이미 가입된 유저입니다."),
```

### 2.3 응답 형식
```json
{
  "is_success": false,
  "code": "USER-007",
  "message": "이미 가입된 유저입니다.",
  "payload": null
}
```

---

## 3. 기술적 고려사항

### 3.1 409 Conflict vs 200 OK

| 방식 | 장점 | 단점 |
|------|------|------|
| **409 Conflict** | RESTful 표준 준수, 명확한 에러 상태 표현 | 클라이언트에서 에러 핸들링 필요 |
| **200 OK + 기존 토큰 반환** | 클라이언트 구현 단순, 멱등성 보장 | 의미적으로 모호함 |

**선택: 409 Conflict**
- 온보딩은 최초 1회만 수행되어야 하는 명확한 요구사항
- 프론트엔드에서 중복 호출 자체를 막아야 하므로 명시적 에러가 적절
- 가이드 조회 로그(`_IS_ALREADY_VIEWED_GUIDE`)도 동일한 패턴 사용 중

### 3.2 Race Condition 대응 전략

| 전략 | 적용 여부 | 이유 |
|------|-----------|------|
| **Application-level 체크** | O (1차) | 대부분의 중복 호출 방어, 명확한 에러 메시지 |
| **DB Unique Constraint** | O (2차, 기존) | 최종 방어선, 동시성 문제 해결 |
| **Distributed Lock** | X | 오버엔지니어링, 온보딩은 빈번한 작업 아님 |

**이유**:
- 온보딩은 유저당 1회만 발생하는 저빈도 작업
- DB unique constraint가 이미 존재하여 race condition 발생 시에도 데이터 정합성 보장
- 분산 락은 결제, 재고 관리 등 고빈도 동시성 작업에 적합

---

## 4. 사이드이펙트 분석

### 4.1 영향 범위
- `UserService.onboardUser()` 메서드만 수정
- `UserErrorStatus` 열거형에 새 에러 코드 추가
- 기존 API 스펙 변경 없음 (에러 응답 코드만 변경: 500 → 409)

### 4.2 하위 호환성
- 기존에 500 에러를 받던 케이스가 409로 변경됨
- 프론트엔드에서 409 에러 핸들링 필요 (이미 가입된 상태이므로 로그인 유도 등)

### 4.3 테스트 필요 사항
- [ ] 정상 온보딩 시나리오 (신규 유저)
- [ ] 중복 온보딩 시나리오 (이미 가입된 provider_id)
- [ ] 동시 호출 시나리오 (race condition 테스트)

---

## 5. 참고 자료

### Best Practices
- [Designing Idempotent APIs in Spring Boot](https://dev.to/devcorner/designing-idempotent-apis-in-spring-boot-2fhi)
- [REST API Idempotency](https://restfulapi.net/idempotent-rest-apis/)
- [409 Conflict 사용 가이드](https://dev.to/jj/solving-the-conflict-of-using-the-http-status-409-2iib)

### 관련 이슈
- 프론트엔드 중복 호출 방지: `[FE] 온보딩 & 가이드 조회 중복 호출로 인한 오류`

---

## 6. 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `UserErrorStatus.java` | `_ALREADY_REGISTERED_USER` 에러 코드 추가 |
| `UserRepository.java` | `existsByProviderId()` 메서드 추가 |
| `UserService.java` | `onboardUser()` 메서드에 중복 체크 로직 추가 |
