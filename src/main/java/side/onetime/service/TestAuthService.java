package side.onetime.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.RefreshToken;
import side.onetime.dto.test.request.TestLoginRequest;
import side.onetime.dto.test.response.TestTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TestErrorStatus;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.util.JwtUtil;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestAuthService {

    @Value("${test.auth.secret-key}")
    private String testSecretKey;

    @Value("${test.auth.user-id}")
    private Long testUserId;

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 테스트 로그인 API.
     *
     * 시크릿 키를 검증한 후, 테스트 유저의 Access/Refresh Token을 발급합니다.
     *
     * @param request 테스트 로그인 요청 (시크릿 키 포함)
     * @return Access Token과 Refresh Token을 포함하는 응답 객체
     */
	@Transactional
    public TestTokenResponse login(TestLoginRequest request) {
        // 1. 시크릿 키 검증
        validateSecretKey(request.secretKey());

        // 2. 고정된 테스트 유저 ID로 토큰 생성
        String browserId = jwtUtil.hashUserAgent("E2E-Test-Agent");

        // 기존 브라우저의 ACTIVE 토큰 revoke
        refreshTokenRepository.revokeByUserIdAndBrowserId(testUserId, "USER", browserId);

        // 새 토큰 생성
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateAccessToken(testUserId, "USER");
        String refreshTokenValue = jwtUtil.generateRefreshToken(testUserId, "USER", browserId, jti);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryAt = jwtUtil.calculateRefreshTokenExpiryAt(now);

        // 3. Refresh Token MySQL 저장
        RefreshToken refreshToken = RefreshToken.create(
                testUserId, "USER", jti, browserId, refreshTokenValue,
                now, expiryAt, "127.0.0.1", "E2E-Test-Agent"
        );
        refreshTokenRepository.save(refreshToken);

        return TestTokenResponse.of(accessToken, refreshToken.getTokenValue());
    }

    /**
     * 만료된 액세스 토큰 발급 API.
     *
     * 시크릿 키를 검증한 후, 이미 만료된 액세스 토큰을 발급합니다.
     * E2E 테스트에서 401 처리, 토큰 재발급 플로우 테스트에 사용됩니다.
     *
     * @param request 테스트 로그인 요청 (시크릿 키 포함)
     * @return 만료된 Access Token을 포함하는 응답 객체
     */
    public TestTokenResponse generateExpiredToken(TestLoginRequest request) {
        // 1. 시크릿 키 검증
        validateSecretKey(request.secretKey());

        // 2. 만료된 액세스 토큰 생성
        String expiredToken = jwtUtil.generateExpiredAccessToken(testUserId, "USER");

        return TestTokenResponse.ofAccessToken(expiredToken);
    }

    private void validateSecretKey(String secretKey) {
        if (!testSecretKey.equals(secretKey)) {
            throw new CustomException(TestErrorStatus._INVALID_SECRET_KEY);
        }
    }
}
