package side.onetime.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.repository.RefreshTokenRepository;

/**
 * Refresh Token 정리 스케줄러
 *
 * - 만료된 토큰 상태 업데이트 (ACTIVE → EXPIRED)
 * - 오래된 비활성 토큰 hard delete (REVOKED/EXPIRED/ROTATED 물리적 삭제)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${refresh-token.cleanup.retention-days:30}")
    private int retentionDays;

    /**
     * 만료된 토큰 상태 업데이트
     *
     * ACTIVE 상태이면서 expiry_at이 지난 토큰을 EXPIRED로 변경
     */
    @Scheduled(cron = "${refresh-token.cleanup.update-expired-cron:0 0 3 * * *}")
    @Transactional
    public void updateExpiredTokens() {
        int count = refreshTokenRepository.updateExpiredTokens(LocalDateTime.now());
        log.info("[RefreshToken Cleanup] 만료 토큰 상태 업데이트: {}건", count);
    }

    /**
     * 오래된 비활성 토큰 hard delete
     *
     * REVOKED, EXPIRED, ROTATED 상태이면서 retention-days 이상 지난 토큰을 물리적으로 삭제
     */
    @Scheduled(cron = "${refresh-token.cleanup.hard-delete-cron:0 30 3 * * *}")
    @Transactional
    public void hardDeleteOldInactiveTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int count = refreshTokenRepository.hardDeleteOldInactiveTokens(threshold);
        log.info("[RefreshToken Cleanup] 오래된 토큰 hard delete: {}건 (retention: {}일)", count, retentionDays);
    }
}
