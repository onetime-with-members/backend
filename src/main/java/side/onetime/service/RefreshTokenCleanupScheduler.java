package side.onetime.service;

import java.time.LocalDateTime;

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
 * - Hard delete는 통계(MAU/DAU)에서 last_used_at을 사용하므로 비활성화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

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
}
