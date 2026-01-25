package side.onetime.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.domain.enums.TokenStatus;
import side.onetime.global.common.dao.BaseEntity;

/**
 * Refresh Token 엔티티
 *
 * Token Rotation 추적 및 사용 이력 로깅을 위한 테이블
 * - family_id: 로그인 세션 단위로 토큰 패밀리 관리 (UUID)
 * - jti: JWT 고유 식별자 (조회 키)
 * - status: 토큰 상태 (ACTIVE, REVOKED, EXPIRED, ROTATED)
 * - Hard Delete: 오래된 비활성 토큰은 물리적으로 삭제
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_family", columnList = "family_id"),
        @Index(name = "idx_refresh_token_user_browser", columnList = "users_id, browser_id"),
        @Index(name = "idx_refresh_token_expiry", columnList = "expiry_at")
})
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

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

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
    @Column(nullable = false, length = 20)
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
     *
     * @param userId      사용자 ID
     * @param userType    사용자 타입 (USER, ADMIN)
     * @param jti         JWT 고유 식별자
     * @param browserId   브라우저 식별자 (User-Agent 해시)
     * @param tokenValue  Refresh Token JWT 문자열
     * @param issuedAt    발급 시각
     * @param expiryAt    만료 시각
     * @param userIp      발급 시 IP
     * @param userAgent   발급 시 User-Agent
     * @return 새로 생성된 RefreshToken 엔티티
     */
    public static RefreshToken create(Long userId, String userType, String jti, String browserId,
                                      String tokenValue, LocalDateTime issuedAt,
                                      LocalDateTime expiryAt, String userIp,
                                      String userAgent) {
        RefreshToken token = new RefreshToken();
        token.familyId = UUID.randomUUID().toString();
        token.userId = userId;
        token.userType = userType;
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
     *
     * @param newJti        새 JWT 고유 식별자
     * @param newTokenValue 새 Refresh Token JWT 문자열
     * @param newIssuedAt   새 발급 시각
     * @param newExpiryAt   새 만료 시각
     * @param newUserIp     새 발급 시 IP
     * @param newUserAgent  새 발급 시 User-Agent
     * @return 로테이션된 새 RefreshToken 엔티티 (같은 family_id, userType 유지)
     */
    public RefreshToken rotate(String newJti, String newTokenValue,
                               LocalDateTime newIssuedAt, LocalDateTime newExpiryAt,
                               String newUserIp, String newUserAgent) {
        RefreshToken token = new RefreshToken();
        token.familyId = this.familyId;
        token.userId = this.userId;
        token.userType = this.userType;
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
     *
     * @param lastUsedIp 마지막 사용 IP
     */
    public void markAsRotated(String lastUsedIp) {
        this.status = TokenStatus.ROTATED;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedIp = lastUsedIp;
    }

    /**
     * 토큰이 활성 상태인지 확인
     *
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this.status == TokenStatus.ACTIVE;
    }
}
