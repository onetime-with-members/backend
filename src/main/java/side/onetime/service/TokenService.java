package side.onetime.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.enums.TokenStatus;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.util.ClientInfoExtractor;
import side.onetime.util.JwtUtil;

/**
 * 토큰 서비스
 *
 * Token Rotation + Grace Period를 적용한 Refresh Token 재발급 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int GRACE_PERIOD_SECONDS = 3;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final ClientInfoExtractor clientInfoExtractor;

    /**
     * 리프레시 토큰으로 액세스/리프레시 토큰을 재발행 하는 메서드.
     *
     * Token Rotation 전략 적용:
     * - ACTIVE 토큰 → 정상 재발급, 기존 토큰은 ROTATED 처리 (원자적 업데이트)
     * - ROTATED 토큰 (Grace Period 내) → 중복 요청으로 간주, 429 에러
     * - ROTATED 토큰 (Grace Period 초과) → 공격 탐지, family 전체 revoke
     * - REVOKED/EXPIRED 토큰 → 재로그인 필요
     *
     * @param reissueTokenRequest 요청 객체 (리프레시 토큰 포함)
     * @param httpRequest HttpServletRequest (IP, User-Agent 추출용)
     * @return 새 액세스/리프레시 토큰
     * @throws CustomException 유효하지 않은 토큰이거나 요청이 너무 잦을 경우
     */
    @Transactional
    public ReissueTokenResponse reissueToken(ReissueTokenRequest reissueTokenRequest, HttpServletRequest httpRequest) {
        String refreshToken = reissueTokenRequest.refreshToken();

        String jti = jwtUtil.getClaimFromToken(refreshToken, "jti", String.class);
        String userIp = clientInfoExtractor.extractClientIp(httpRequest);
        String userAgent = clientInfoExtractor.extractUserAgent(httpRequest);

        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new CustomException(TokenErrorStatus._NOT_FOUND_REFRESH_TOKEN));

        // 토큰 값 검증: DB에 저장된 토큰과 요청 토큰 비교
        if (!token.getTokenValue().equals(refreshToken)) {
            throw new CustomException(TokenErrorStatus._INVALID_REFRESH_TOKEN);
        }

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

    /**
     * Token Rotation 수행 (원자적 업데이트)
     *
     * 기존 토큰을 ROTATED 상태로 변경하고 새 토큰을 생성합니다.
     * 원자적 업데이트를 사용하여 동시 요청 시 race condition을 방지합니다.
     *
     * @param oldToken 기존 토큰
     * @param userIp 요청 IP
     * @param userAgent 요청 User-Agent
     * @return 새 토큰 응답
     * @throws CustomException 토큰이 이미 사용된 경우 (동시 요청으로 인한 race condition)
     */
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

    /**
     * Grace Period (3초) 내인지 확인
     *
     * @param token 확인할 토큰
     * @return Grace Period 내 여부
     */
    private boolean isWithinGracePeriod(RefreshToken token) {
        return token.getLastUsedAt() != null &&
                token.getLastUsedAt().plusSeconds(GRACE_PERIOD_SECONDS).isAfter(LocalDateTime.now());
    }
}
