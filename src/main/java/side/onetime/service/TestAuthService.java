package side.onetime.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.RefreshToken;
import side.onetime.dto.test.request.TestLoginRequest;
import side.onetime.dto.user.response.OnboardUserResponse;
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
    public OnboardUserResponse login(TestLoginRequest request) {
        // 1. 시크릿 키 검증
        if (!testSecretKey.equals(request.secretKey())) {
            throw new CustomException(TestErrorStatus._INVALID_SECRET_KEY);
        }

        // 2. 고정된 테스트 유저 ID로 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(testUserId, "USER");
        String browserId = jwtUtil.hashUserAgent("E2E-Test-Agent");
        String refreshToken = jwtUtil.generateRefreshToken(testUserId, browserId);

        // 3. Refresh Token Redis 저장
        RefreshToken token = new RefreshToken(testUserId, browserId, refreshToken);
        refreshTokenRepository.save(token);

        return OnboardUserResponse.of(accessToken, refreshToken);
    }
}
