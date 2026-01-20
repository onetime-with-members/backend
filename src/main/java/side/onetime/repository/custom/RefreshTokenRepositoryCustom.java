package side.onetime.repository.custom;

import java.time.LocalDateTime;

public interface RefreshTokenRepositoryCustom {

    /**
     * 특정 사용자 + 브라우저의 ACTIVE 토큰을 REVOKED로 변경 (로그아웃)
     *
     * @param userId    사용자 ID
     * @param browserId 브라우저 식별자
     */
    void revokeByUserIdAndBrowserId(Long userId, String browserId);

    /**
     * 특정 사용자의 모든 ACTIVE 토큰을 REVOKED로 변경 (전체 로그아웃, 탈퇴)
     *
     * @param userId 사용자 ID
     */
    void revokeAllByUserId(Long userId);

    /**
     * 특정 토큰 패밀리의 모든 ACTIVE/ROTATED 토큰을 REVOKED로 변경 (공격 탐지 시)
     *
     * @param familyId 토큰 패밀리 ID
     */
    void revokeAllByFamilyId(String familyId);

    /**
     * 만료된 ACTIVE 토큰을 EXPIRED로 변경
     *
     * @param now 현재 시각
     * @return 변경된 토큰 수
     */
    int updateExpiredTokens(LocalDateTime now);

    /**
     * 오래된 비활성 토큰을 삭제 (Hard Delete)
     *
     * @param threshold 기준 시각 (이 시각 이전에 수정된 토큰 대상)
     * @return 삭제된 토큰 수
     */
    int hardDeleteOldInactiveTokens(LocalDateTime threshold);
}
