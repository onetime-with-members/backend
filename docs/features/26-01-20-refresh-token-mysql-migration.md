# Refresh Token MySQL 마이그레이션

> **상태**: ✅ 구현 완료
> **목적**: Redis에서 관리하던 Refresh Token을 MySQL로 마이그레이션하여 Token Rotation 추적 및 로깅 기능 추가

## 1. 배경

### 현재 상태 (AS-IS)
- Refresh Token을 Redis에 저장 (`refreshToken:{userId}` 키에 리스트 형태)
- `browserId:refreshToken` 형태로 저장하여 다중 브라우저 지원
- 쿨다운 기능 존재 (0.5초)
- 단순 key-value 저장으로 토큰 사용 이력 추적 불가
- Token Rotation 공격 탐지 불가

### 변경 후 (TO-BE)
- MySQL `refresh_token` 테이블에서 관리
- 토큰 패밀리(family_id) 기반 계보 관리
- 발급/사용 이력 로깅 (IP, User-Agent 등)
- Token Rotation 탐지 가능
- `userId + browserId` 조합으로 고유성 판단

### Redis 사용 현황

| Repository | 용도 | 마이그레이션 |
|------------|------|-------------|
| RefreshTokenRepository | Refresh Token 저장 | **MySQL로 이전** |
| LettuceLockRepository | 분산 락 (@DistributedLock) | 토큰 재발급에서 **제거** (DB로 대체) |

> **결론**: Token Rotation + Grace Period 도입으로 분산락/쿨다운 불필요. Redis 의존성 최소화.

---

## 2. 데이터베이스 설계

### refresh_token 테이블

```sql
CREATE TABLE refresh_token (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'PK',
    family_id     VARCHAR(36)                        NOT NULL COMMENT '토큰 패밀리 ID (UUID, 로그인 세션 단위)',
    users_id      BIGINT                             NOT NULL COMMENT '토큰 소유 사용자 ID',
    jti           VARCHAR(128)                       NOT NULL COMMENT 'JWT 고유 식별자',
    browser_id    VARCHAR(256)                       NOT NULL COMMENT '브라우저 식별자 (User-Agent 해시)',
    user_agent    VARCHAR(512)                       NULL COMMENT '발급 시 User-Agent',
    user_ip       VARCHAR(45)                        NULL COMMENT '발급 시 IP',
    token_value   TEXT                               NOT NULL COMMENT 'Refresh Token JWT',
    status        ENUM('ACTIVE', 'REVOKED', 'EXPIRED', 'ROTATED')
                            DEFAULT 'ACTIVE'         NOT NULL COMMENT '토큰 상태',
    issued_at     DATETIME                           NOT NULL COMMENT '발급 시각',
    expiry_at     DATETIME                           NOT NULL COMMENT '만료 시각',
    last_used_at  DATETIME                           NULL COMMENT '마지막 사용 시각',
    last_used_ip  VARCHAR(45)                        NULL COMMENT '마지막 사용 IP',
    reissue_count INT          DEFAULT 0             NOT NULL COMMENT '재발급 횟수',
    created_date  DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    updated_date  DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL COMMENT '수정일시',

    CONSTRAINT uk_refresh_token_jti UNIQUE (jti)
) COMMENT 'Refresh Token 관리 테이블 (Token Rotation, 로깅)';

CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_user_browser ON refresh_token (users_id, browser_id);
CREATE INDEX idx_refresh_token_expiry ON refresh_token (expiry_at);
CREATE INDEX idx_refresh_token_status_updated ON refresh_token (status, updated_date);
```

### 컬럼 설명

| 컬럼 | 설명 | 비고 |
|------|------|------|
| id | PK | auto_increment |
| family_id | 토큰 패밀리 ID | UUID, 로그인 세션 단위로 공유 |
| users_id | 사용자 ID | users.users_id 참조 (FK 없음) |
| jti | JWT ID | JWT 페이로드의 jti claim |
| browser_id | 브라우저 식별자 | User-Agent 해시값 (기존 로직 유지) |
| user_agent | User-Agent | 발급 시점 기록 (원본) |
| user_ip | IP 주소 | 발급 시점 기록 |
| token_value | JWT 문자열 | Refresh Token 전체 |
| status | 토큰 상태 | ACTIVE, REVOKED, EXPIRED, ROTATED |
| issued_at | 발급 시각 | JWT의 iat claim |
| expiry_at | 만료 시각 | JWT의 exp claim |
| last_used_at | 마지막 사용 시각 | 토큰 재발급 요청 시 갱신 |
| last_used_ip | 마지막 사용 IP | 토큰 재발급 요청 시 갱신 |
| reissue_count | 재발급 횟수 | Token Rotation 횟수 |

### 인덱스 설명

| 인덱스 | 용도 |
|--------|------|
| uk_refresh_token_jti | jti 조회 (unique constraint) |
| idx_refresh_token_family | 토큰 패밀리 전체 무효화 |
| idx_refresh_token_user_browser | 로그인/로그아웃 시 사용자+브라우저 조회 |
| idx_refresh_token_expiry | 만료 토큰 배치 정리 |
| idx_refresh_token_status_updated | 스케줄러 정리 작업 (status + updated_date 복합 조건) |

### status 상태 전이

```
ACTIVE ──┬── logout ──> REVOKED ──┬
         │                        │
         ├── refresh (정상) ──> ROTATED ──┼── 30일 경과 ──> (Hard Delete)
         │                        │
         └── 만료 ──> EXPIRED ────┘
```

### TokenStatus enum

```java
@Getter
@RequiredArgsConstructor
public enum TokenStatus {
    ACTIVE,     // 활성 (사용 가능)
    REVOKED,    // 폐기 (로그아웃, 강제 만료)
    EXPIRED,    // 만료 (자연 만료)
    ROTATED     // 로테이션 (Token Rotation으로 새 토큰 발급됨)
}
```

> **참고**: 기존 설계에서 DELETED 상태(soft delete)를 사용하려 했으나, 30일 이상된 비활성 토큰은 보존 가치가 낮아 Hard Delete로 변경

---

## 3. Token Rotation 전략

### 3.1 family_id 기반 토큰 관리

하나의 로그인 세션에서 파생된 모든 토큰이 동일한 `family_id`를 공유:

```
로그인 → Token A (family_id=UUID-1)
재발급 → Token B (family_id=UUID-1)  ← 동일한 family_id
재발급 → Token C (family_id=UUID-1)
```

### 3.2 정상 플로우

```
1. 로그인/OAuth 성공
   └── 새 RefreshToken 생성 (status=ACTIVE, family_id=새 UUID 생성)

2. 토큰 재발급 (refresh)
   ├── 기존 토큰 조회 (jti로)
   ├── 기존 토큰 status를 ROTATED로 변경
   ├── last_used_at, last_used_ip 갱신
   └── 새 토큰 생성 (status=ACTIVE, family_id=기존 family_id 유지, reissue_count+1)

3. 로그아웃
   └── 해당 사용자+브라우저의 ACTIVE 토큰을 REVOKED로 변경
```

### 3.3 Token Rotation 공격 탐지 + Grace Period

이미 ROTATED된 토큰으로 재발급 요청이 오면:
- **Grace Period (3초) 이내**: 프론트 중복 요청으로 간주 → 무시 (429 에러)
- **Grace Period 초과**: 공격 탐지 → family 전체 REVOKED → 강제 재로그인

```
공격 시나리오:
1. 공격자가 RefreshToken A 탈취 (family_id=UUID-1)
2. 정상 사용자가 A로 재발급 → B 발급 (A는 ROTATED, last_used_at 기록)
3. 공격자가 A로 재발급 시도
   - 3초 이내: 중복 요청으로 간주 → 429 에러 (현실적으로 불가능한 케이스)
   - 3초 이후: 공격 탐지 → family 전체 REVOKED
4. UPDATE ... WHERE family_id = 'UUID-1' → 한 방에 전체 무효화
```

### 3.4 Grace Period 도입 이유

| 문제 | 해결 |
|------|------|
| 프론트 버그로 동시 재발급 요청 | 3초 내 중복 → 429 에러, 로그아웃 안 됨 |
| 분산락/쿨다운 Redis 의존성 | DB의 `last_used_at`으로 대체 |
| 3초 내 탈취+재발급 공격 | 현실적으로 불가능 (매우 정교한 자동화 필요) |

> **참고**: Auth0도 유사한 "leeway/grace period" 개념 사용

---

## 4. last_used_at / last_used_ip 활용 방안

### 4.1 기본 로깅 (MVP)
- 토큰 재발급 시 `last_used_at`, `last_used_ip` 갱신
- 관리자 대시보드에서 사용자별 토큰 사용 현황 조회

### 4.2 이상 탐지 (향후 확장)

| 시나리오 | 탐지 방법 | 대응 |
|----------|----------|------|
| 다른 IP에서 재발급 | `user_ip != last_used_ip` | 경고 로깅 / 알림 |
| 짧은 시간 내 다수 재발급 | `reissue_count` 급증 | 토큰 무효화 고려 |
| 비정상 지역 접근 | IP Geolocation | 추가 인증 요구 (선택) |

### 4.3 감사 로그 (Audit Trail)
- 사용자 계정 이상 발생 시 토큰 사용 이력 추적
- 언제, 어디서 토큰이 사용되었는지 확인 가능
- `family_id`로 그룹화 후 `issued_at` 정렬하면 토큰 발급 순서 파악 가능

---

## 5. 구현 상세

### 5.1 수정 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `RefreshToken.java` | Redis용 → JPA 엔티티로 전환 |
| `RefreshTokenRepository.java` | Redis 기반 → JpaRepository + Custom 인터페이스 |
| `TokenService.java` | Token Rotation + Grace Period 로직으로 변경, @DistributedLock 제거 |
| `UserService.java` | 로그아웃 시 토큰 revoke 로직 수정 |
| `OAuthLoginSuccessHandler.java` | 토큰 저장 로직 수정 |
| `TestAuthService.java` | 토큰 저장 로직 수정 |
| `JwtUtil.java` | jti claim 추가 |
| `UserRepositoryImpl.java` | withdraw()에 refreshToken 삭제 추가 |
| `TokenController.java` | HttpServletRequest 파라미터 추가 (IP, User-Agent 추출용) |

### 5.2 신규 생성 파일

```
side.onetime/
├── domain/
│   ├── RefreshToken.java              # JPA 엔티티 (기존 파일 전면 수정)
│   └── enums/
│       └── TokenStatus.java           # ACTIVE, REVOKED, EXPIRED, ROTATED (신규)
├── repository/
│   ├── RefreshTokenRepository.java    # JpaRepository + Custom (기존 파일 전면 수정)
│   └── custom/
│       ├── RefreshTokenRepositoryCustom.java  # 커스텀 메서드 인터페이스 (신규)
│       └── RefreshTokenRepositoryImpl.java    # QueryDSL 구현체 (신규)
├── util/
│   └── ClientInfoExtractor.java       # IP, User-Agent 추출 유틸리티 (신규)
└── scheduler/
    └── RefreshTokenCleanupScheduler.java  # 만료 토큰 정리 스케줄러 (신규)
```

> **삭제 대상**: 기존 Redis 기반 쿨다운 로직 (RefreshTokenRepository 내 cooldown 관련 메서드)

### 5.3 엔티티 설계

```java
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "users_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 128)
    private String jti;

    @Column(name = "browser_id", nullable = false, length = 256)
    private String browserId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "token_value", nullable = false, columnDefinition = "TEXT")
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    @Column(name = "reissue_count", nullable = false)
    private int reissueCount;

    /**
     * 신규 Refresh Token 생성 (로그인 시)
     */
    public static RefreshToken create(Long userId, String jti, String browserId,
                                      String tokenValue, LocalDateTime issuedAt,
                                      LocalDateTime expiryAt, String userIp,
                                      String userAgent) {
        RefreshToken token = new RefreshToken();
        token.familyId = UUID.randomUUID().toString();  // 새 패밀리 생성
        token.userId = userId;
        token.jti = jti;
        token.browserId = browserId;
        token.tokenValue = tokenValue;
        token.status = TokenStatus.ACTIVE;
        token.issuedAt = issuedAt;
        token.expiryAt = expiryAt;
        token.userIp = userIp;
        token.userAgent = userAgent;
        token.reissueCount = 0;
        return token;
    }

    /**
     * Token Rotation으로 새 토큰 생성 (재발급 시)
     */
    public RefreshToken rotate(String newJti, String newTokenValue,
                               LocalDateTime newIssuedAt, LocalDateTime newExpiryAt,
                               String newUserIp, String newUserAgent) {
        RefreshToken token = new RefreshToken();
        token.familyId = this.familyId;  // 기존 family_id 유지
        token.userId = this.userId;
        token.jti = newJti;
        token.browserId = this.browserId;
        token.tokenValue = newTokenValue;
        token.status = TokenStatus.ACTIVE;
        token.issuedAt = newIssuedAt;
        token.expiryAt = newExpiryAt;
        token.userIp = newUserIp;
        token.userAgent = newUserAgent;
        token.reissueCount = this.reissueCount + 1;
        return token;
    }

    /**
     * 토큰 상태를 ROTATED로 변경 (Token Rotation 시)
     */
    public void markAsRotated(String lastUsedIp) {
        this.status = TokenStatus.ROTATED;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedIp = lastUsedIp;
    }

    /**
     * 토큰이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == TokenStatus.ACTIVE;
    }
}
```

### 5.4 Repository 설계

**RefreshTokenRepository.java**
```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepositoryCustom {

    Optional<RefreshToken> findByJti(String jti);

    /**
     * 원자적 업데이트: ACTIVE 상태인 경우에만 ROTATED로 변경
     * Race condition 방지를 위해 WHERE 절에서 상태 체크
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'ROTATED',
            r.lastUsedAt = :lastUsedAt,
            r.lastUsedIp = :lastUsedIp,
            r.updatedDate = :lastUsedAt
        WHERE r.id = :tokenId
          AND r.status = 'ACTIVE'
    """)
    int markAsRotatedIfActive(@Param("tokenId") Long tokenId,
                              @Param("lastUsedAt") LocalDateTime lastUsedAt,
                              @Param("lastUsedIp") String lastUsedIp);
}
```

**RefreshTokenRepositoryCustom.java**
```java
public interface RefreshTokenRepositoryCustom {

    void revokeByUserIdAndBrowserId(Long userId, String browserId);

    void revokeAllByUserId(Long userId);

    void revokeAllByFamilyId(String familyId);

    int updateExpiredTokens(LocalDateTime now);

    int hardDeleteOldInactiveTokens(LocalDateTime threshold);
}
```

**RefreshTokenRepositoryImpl.java**
```java
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public void revokeByUserIdAndBrowserId(Long userId, String browserId) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .where(refreshToken.userId.eq(userId)
                        .and(refreshToken.browserId.eq(browserId))
                        .and(refreshToken.status.eq(TokenStatus.ACTIVE)))
                .execute();
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .where(refreshToken.userId.eq(userId)
                        .and(refreshToken.status.eq(TokenStatus.ACTIVE)))
                .execute();
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .where(refreshToken.familyId.eq(familyId)
                        .and(refreshToken.status.in(TokenStatus.ACTIVE, TokenStatus.ROTATED)))
                .execute();
    }

    @Override
    public int updateExpiredTokens(LocalDateTime now) {
        return (int) queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.EXPIRED)
                .where(refreshToken.status.eq(TokenStatus.ACTIVE)
                        .and(refreshToken.expiryAt.lt(now)))
                .execute();
    }

    @Override
    public int hardDeleteOldInactiveTokens(LocalDateTime threshold) {
        return (int) queryFactory.delete(refreshToken)
                .where(refreshToken.status.in(TokenStatus.REVOKED, TokenStatus.EXPIRED, TokenStatus.ROTATED)
                        .and(refreshToken.updatedDate.lt(threshold)))
                .execute();
    }
}
```

### 5.5 JwtUtil 변경사항

JWT에 `jti` claim 추가:

```java
public String generateRefreshToken(Long userId, String browserId) {
    String jti = UUID.randomUUID().toString();  // 고유 식별자 생성

    return Jwts.builder()
            .claim("userId", userId)
            .claim("browserId", browserId)
            .claim("jti", jti)  // 추가
            .issuedAt(new Date())
            .expiration(new Date(...))
            .signWith(this.getSigningKey())
            .compact();
}
```

### 5.6 ClientInfoExtractor 유틸리티

```java
@Component
public class ClientInfoExtractor {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR"
    };

    public String extractClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return null;
        return userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
    }
}
```

---

## 6. TokenService 구현 (Grace Period 적용)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int GRACE_PERIOD_SECONDS = 3;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final ClientInfoExtractor clientInfoExtractor;

    @Transactional
    public ReissueTokenResponse reissueToken(ReissueTokenRequest request, HttpServletRequest httpRequest) {
        String jti = jwtUtil.getClaimFromToken(request.refreshToken(), "jti", String.class);
        String userIp = clientInfoExtractor.extractClientIp(httpRequest);
        String userAgent = clientInfoExtractor.extractUserAgent(httpRequest);

        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new CustomException(TokenErrorStatus._NOT_FOUND_REFRESH_TOKEN));

        // 1. ACTIVE 토큰 → 정상 재발급
        if (token.isActive()) {
            return rotateToken(token, userIp, userAgent);
        }

        // 2. ROTATED 토큰 → Grace Period 체크
        if (token.getStatus() == TokenStatus.ROTATED) {
            if (isWithinGracePeriod(token)) {
                // 중복 요청 → 무시
                throw new CustomException(TokenErrorStatus._DUPLICATED_REQUEST);
            } else {
                // 공격 탐지 → family 전체 revoke
                log.warn("[Token Reuse Detected] familyId={}, jti={}, ip={}",
                        token.getFamilyId(), jti, userIp);
                refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId());
                throw new CustomException(TokenErrorStatus._TOKEN_REUSE_DETECTED);
            }
        }

        // 3. REVOKED, EXPIRED → 재로그인 필요
        throw new CustomException(TokenErrorStatus._INVALID_REFRESH_TOKEN);
    }

    private ReissueTokenResponse rotateToken(RefreshToken oldToken, String userIp, String userAgent) {
        LocalDateTime now = LocalDateTime.now();

        // 원자적 업데이트: ACTIVE 상태인 경우에만 ROTATED로 변경
        int updated = refreshTokenRepository.markAsRotatedIfActive(oldToken.getId(), now, userIp);
        if (updated == 0) {
            // 이미 다른 요청에서 토큰을 rotate 했음 (race condition)
            throw new CustomException(TokenErrorStatus._ALREADY_USED_REFRESH_TOKEN);
        }

        // 새 토큰 생성
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtUtil.generateAccessToken(oldToken.getUserId(), "USER");
        String newRefreshToken = jwtUtil.generateRefreshToken(oldToken.getUserId(), oldToken.getBrowserId(), newJti);

        LocalDateTime expiryAt = jwtUtil.calculateRefreshTokenExpiryAt(now);

        RefreshToken newToken = oldToken.rotate(newJti, newRefreshToken, now, expiryAt, userIp, userAgent);
        refreshTokenRepository.save(newToken);

        return ReissueTokenResponse.of(newAccessToken, newRefreshToken);
    }

    private boolean isWithinGracePeriod(RefreshToken token) {
        return token.getLastUsedAt() != null &&
               token.getLastUsedAt().plusSeconds(GRACE_PERIOD_SECONDS).isAfter(LocalDateTime.now());
    }
}
```

> **핵심**: 분산락/쿨다운 없이 DB의 `status`와 `last_used_at`만으로 동시성 및 공격 탐지 처리

---

## 7. 스케줄러 구현

### YAML 설정

```yaml
refresh-token:
  cleanup:
    update-expired-cron: ${REFRESH_TOKEN_UPDATE_EXPIRED_CRON:0 0 3 * * *}
    hard-delete-cron: ${REFRESH_TOKEN_HARD_DELETE_CRON:0 30 3 * * *}
    retention-days: ${REFRESH_TOKEN_RETENTION_DAYS:30}
```

### 스케줄러 코드

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${refresh-token.cleanup.retention-days:30}")
    private int retentionDays;

    /**
     * 만료된 토큰 상태 업데이트
     * ACTIVE 상태이면서 expiry_at이 지난 토큰 → EXPIRED
     */
    @Scheduled(cron = "${refresh-token.cleanup.update-expired-cron:0 0 3 * * *}")
    @Transactional
    public void updateExpiredTokens() {
        int count = refreshTokenRepository.updateExpiredTokens(LocalDateTime.now());
        log.info("[RefreshToken Cleanup] 만료 토큰 상태 업데이트: {}건", count);
    }

    /**
     * 오래된 비활성 토큰 hard delete
     * REVOKED, EXPIRED, ROTATED 상태이면서 retention-days 이상 지난 토큰 → 물리적 삭제
     */
    @Scheduled(cron = "${refresh-token.cleanup.hard-delete-cron:0 30 3 * * *}")
    @Transactional
    public void hardDeleteOldInactiveTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int count = refreshTokenRepository.hardDeleteOldInactiveTokens(threshold);
        log.info("[RefreshToken Cleanup] 오래된 토큰 hard delete: {}건 (retention: {}일)", count, retentionDays);
    }
}
```

---

## 8. 사용자 탈퇴 시 처리

`UserRepositoryImpl.withdraw()` 메서드에 refreshToken 삭제 로직 추가:

```java
@Override
public void withdraw(User activeUser) {
    // ... 기존 삭제 로직 ...

    // RefreshToken 삭제 추가
    queryFactory.update(refreshToken)
            .set(refreshToken.status, TokenStatus.REVOKED)
            .where(refreshToken.userId.eq(activeUser.getId()))
            .execute();

    // ... 유저 상태 업데이트 ...
}
```

---

## 9. API 변경 사항

외부 API 스펙 변경 없음 (내부 구현만 변경)

---

## 10. 마이그레이션 계획

### Phase 1: 준비 ✅
- [x] DDL 작성 및 테이블 생성
- [x] TokenStatus enum 생성
- [x] RefreshToken JPA 엔티티 구현
- [x] RefreshTokenRepository + Custom + Impl 구현
- [x] ClientInfoExtractor 유틸리티 구현
- [x] JwtUtil에 jti claim 추가, expiryAt 계산 메서드 추출
- [x] TokenService Token Rotation + Grace Period 로직 구현
- [x] TokenService에서 @DistributedLock 제거
- [x] TokenController에 HttpServletRequest 파라미터 추가
- [x] UserService.withdraw()에 토큰 revoke 추가
- [x] OAuthLoginSuccessHandler, TestAuthService 토큰 저장 로직 수정
- [x] UserService.onboardUser()에 IP/UserAgent 저장 수정
- [x] 스케줄러 구현 (cron/retention-days YAML 외부화)
- [x] Testcontainers 테스트 환경 구성
- [x] 통합 테스트 코드 작성

### Phase 2: 배포
- [ ] 테스트 서버 배포 및 검증
- [ ] 프로덕션 DDL 실행
- [ ] 프로덕션 배포

### Phase 3: 정리
- [ ] Redis의 기존 Refresh Token 키 삭제
- [ ] 기존 쿨다운 관련 코드 삭제

---

## 11. 고려 사항

### 11.1 성능
- Refresh Token 재발급은 빈번하지 않음 (Access Token 만료 시에만)
- 인덱스로 조회 성능 보장
- Redis 대비 약간의 latency 증가 예상 (무시할 수준)

### 11.2 동시성
- **분산락 제거**: 원자적 업데이트 + Grace Period로 중복 요청 처리
- 동일 토큰 동시 요청 → `markAsRotatedIfActive` 원자적 업데이트로 첫 번째만 성공
- 나머지 요청은 `_ALREADY_USED_REFRESH_TOKEN` 에러 반환
- 동일 사용자의 여러 브라우저 → `userId + browserId` 조합으로 분리

### 11.3 보안
- Token Rotation으로 탈취 토큰 재사용 탐지
- Grace Period (3초) 내 재사용 → 중복 요청으로 간주 (현실적 공격 불가)
- Grace Period 초과 재사용 → 공격 탐지 → family 전체 revoke
- family_id 기반 계보 전체 무효화로 빠른 대응
- IP, User-Agent 로깅으로 감사 추적 가능

---

## 12. 테스트 환경

### Testcontainers 설정

H2 대신 Testcontainers(MySQL)를 사용하여 실제 운영 환경과 동일한 DB로 테스트:

```java
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class DatabaseTestConfig {

    private static final String MYSQL_IMAGE = "mysql:8.0";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
                .withCommand("--default-time-zone=+09:00")
                .withLogConsumer(new Slf4jLogConsumer(log));
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
    }
}
```

### 테스트 클래스

| 테스트 클래스 | 설명 |
|-------------|------|
| `RefreshTokenRepositoryTest` | Repository 레이어 통합 테스트 |
| `TokenServiceTest` | 토큰 재발급, 검증, 토큰 탈취 감지 테스트 |
| `TokenControllerTest` | API 엔드포인트 테스트 |

---

## 13. 참고 자료

- [JWT Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-jwt-bcp-07)
- [OAuth 2.0 Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)
- [Auth0 Token Family 개념](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

---

## 14. 구현 변경 사항 (설계 대비)

| 항목 | 설계 | 구현 | 이유 |
|------|------|------|------|
| 토큰 삭제 방식 | Soft Delete (DELETED 상태) | Hard Delete (물리 삭제) | 30일 이상 된 비활성 토큰은 보존 가치 낮음 |
| 동시성 처리 | `markAsRotated()` 메서드 | `markAsRotatedIfActive()` 원자적 업데이트 | Race condition 완벽 방지 |
| 스케줄러 설정 | 하드코딩 cron | YAML 외부화 (환경변수 지원) | 환경별 유연한 설정 |
| TokenStatus | 5개 (DELETED 포함) | 4개 (DELETED 제거) | Hard Delete로 변경됨에 따라 불필요 |
