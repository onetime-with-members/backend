package side.onetime.domain.enums;

/**
 * Refresh Token 상태 enum
 *
 * Token Rotation 및 토큰 라이프사이클 관리를 위한 상태 정의
 */
public enum TokenStatus {
    ACTIVE,     // 활성 (사용 가능)
    REVOKED,    // 폐기 (로그아웃, 강제 만료)
    EXPIRED,    // 만료 (자연 만료)
    ROTATED     // 로테이션 (Token Rotation으로 새 토큰 발급됨)
}
