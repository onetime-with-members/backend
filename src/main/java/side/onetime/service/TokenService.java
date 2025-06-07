package side.onetime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import side.onetime.domain.RefreshToken;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.global.lock.annotation.DistributedLock;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /**
     * 리프레시 토큰으로 액세스/리프레시 토큰을 재발행 하는 메서드.
     *
     * - 토큰에서 userId, browserId 추출
     * - Redis에서 기존 리프레시 토큰 유효성 검증
     * - 새로운 토큰 발급 후 Redis에 갱신
     * - 동시에 중복 재발행 요청이 들어오는 경우를 방지하기 위해 browserId 단위로 분산 락(@DistributedLock)을 적용함
     *
     * @param reissueTokenRequest 요청 객체 (리프레시 토큰 포함)
     * @return 새 액세스/리프레시 토큰
     * @throws CustomException 리프레시 토큰이 없거나 일치하지 않을 경우
     */
    @DistributedLock(prefix = "lock:reissue", key = "#reissueTokenRequest.refreshToken", waitTime = 0)
    public ReissueTokenResponse reissueToken(ReissueTokenRequest reissueTokenRequest) {
        String refreshToken = reissueTokenRequest.refreshToken();

        Long userId = jwtUtil.getClaimFromToken(refreshToken, "userId", Long.class);
        String browserId = jwtUtil.getClaimFromToken(refreshToken, "browserId", String.class);
        String existRefreshToken = refreshTokenRepository.findByUserIdAndBrowserId(userId, browserId)
                .orElseThrow(() -> new CustomException(TokenErrorStatus._NOT_FOUND_REFRESH_TOKEN));

        if (!existRefreshToken.equals(refreshToken)) {
            throw new CustomException(TokenErrorStatus._NOT_FOUND_REFRESH_TOKEN);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, "USER");
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, browserId);
        refreshTokenRepository.save(new RefreshToken(userId, browserId, newRefreshToken));

        return ReissueTokenResponse.of(newAccessToken, newRefreshToken);
    }
}
