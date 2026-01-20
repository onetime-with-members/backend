package side.onetime.token;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.enums.TokenStatus;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.service.TokenService;
import side.onetime.util.ClientInfoExtractor;
import side.onetime.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ClientInfoExtractor clientInfoExtractor;

    @Mock
    private HttpServletRequest httpRequest;

    private static final String TEST_JTI = "test-jti-uuid";
    private static final String TEST_REFRESH_TOKEN = "test.refresh.token";
    private static final String TEST_NEW_ACCESS_TOKEN = "new.access.token";
    private static final String TEST_NEW_REFRESH_TOKEN = "new.refresh.token";
    private static final String TEST_USER_IP = "127.0.0.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_BROWSER_ID = "browser-hash-123";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Common mock setup
        given(jwtUtil.getClaimFromToken(TEST_REFRESH_TOKEN, "jti", String.class)).willReturn(TEST_JTI);
        given(clientInfoExtractor.extractClientIp(httpRequest)).willReturn(TEST_USER_IP);
        given(clientInfoExtractor.extractUserAgent(httpRequest)).willReturn(TEST_USER_AGENT);
    }

    private RefreshToken createTestToken(TokenStatus status, LocalDateTime lastUsedAt) {
        RefreshToken token = RefreshToken.create(
                TEST_USER_ID, TEST_JTI, TEST_BROWSER_ID, TEST_REFRESH_TOKEN,
                LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                TEST_USER_IP, TEST_USER_AGENT
        );

        // Use reflection to set status and lastUsedAt for testing
        try {
            Field statusField = RefreshToken.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(token, status);

            if (lastUsedAt != null) {
                Field lastUsedAtField = RefreshToken.class.getDeclaredField("lastUsedAt");
                lastUsedAtField.setAccessible(true);
                lastUsedAtField.set(token, lastUsedAt);
            }

            Field idField = RefreshToken.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(token, 1L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test token fields", e);
        }

        return token;
    }

    @Nested
    @DisplayName("reissueToken 메서드")
    class ReissueToken {

        @Test
        @DisplayName("ACTIVE 토큰으로 재발급 성공")
        void reissueToken_Success_WithActiveToken() {
            // given
            RefreshToken activeToken = createTestToken(TokenStatus.ACTIVE, null);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(activeToken));
            given(refreshTokenRepository.markAsRotatedIfActive(eq(1L), any(LocalDateTime.class), eq(TEST_USER_IP)))
                    .willReturn(1);
            given(jwtUtil.generateAccessToken(TEST_USER_ID, "USER")).willReturn(TEST_NEW_ACCESS_TOKEN);
            given(jwtUtil.generateRefreshToken(eq(TEST_USER_ID), eq(TEST_BROWSER_ID), anyString()))
                    .willReturn(TEST_NEW_REFRESH_TOKEN);
            given(jwtUtil.calculateRefreshTokenExpiryAt(any(LocalDateTime.class)))
                    .willReturn(LocalDateTime.now().plusDays(14));

            // when
            ReissueTokenResponse response = tokenService.reissueToken(request, httpRequest);

            // then
            assertThat(response.accessToken()).isEqualTo(TEST_NEW_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(TEST_NEW_REFRESH_TOKEN);
            verify(refreshTokenRepository).markAsRotatedIfActive(eq(1L), any(LocalDateTime.class), eq(TEST_USER_IP));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("토큰 값 불일치 시 실패")
        void reissueToken_Fail_TokenValueMismatch() {
            // given
            RefreshToken token = createTestToken(TokenStatus.ACTIVE, null);
            // Change tokenValue to simulate mismatch
            try {
                Field tokenValueField = RefreshToken.class.getDeclaredField("tokenValue");
                tokenValueField.setAccessible(true);
                tokenValueField.set(token, "different.token.value");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);
            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(token));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("ROTATED 토큰 - Grace Period 내 중복 요청")
        void reissueToken_Fail_DuplicatedRequestWithinGracePeriod() {
            // given
            LocalDateTime withinGracePeriod = LocalDateTime.now().minusSeconds(1); // 1초 전 (3초 Grace Period 내)
            RefreshToken rotatedToken = createTestToken(TokenStatus.ROTATED, withinGracePeriod);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(rotatedToken));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._DUPLICATED_REQUEST);
                    });
        }

        @Test
        @DisplayName("ROTATED 토큰 - Grace Period 초과 시 공격 탐지")
        void reissueToken_Fail_TokenReuseDetected() {
            // given
            LocalDateTime outsideGracePeriod = LocalDateTime.now().minusSeconds(10); // 10초 전 (Grace Period 초과)
            RefreshToken rotatedToken = createTestToken(TokenStatus.ROTATED, outsideGracePeriod);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(rotatedToken));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._TOKEN_REUSE_DETECTED);
                    });

            verify(refreshTokenRepository).revokeAllByFamilyId(rotatedToken.getFamilyId());
        }

        @Test
        @DisplayName("REVOKED 토큰으로 재발급 시도 시 실패")
        void reissueToken_Fail_WithRevokedToken() {
            // given
            RefreshToken revokedToken = createTestToken(TokenStatus.REVOKED, null);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(revokedToken));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("EXPIRED 토큰으로 재발급 시도 시 실패")
        void reissueToken_Fail_WithExpiredToken() {
            // given
            RefreshToken expiredToken = createTestToken(TokenStatus.EXPIRED, null);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(expiredToken));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 재발급 시도 시 실패")
        void reissueToken_Fail_TokenNotFound() {
            // given
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);
            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._NOT_FOUND_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("원자적 업데이트 실패 시 (Race Condition) 에러")
        void reissueToken_Fail_RaceCondition() {
            // given
            RefreshToken activeToken = createTestToken(TokenStatus.ACTIVE, null);
            ReissueTokenRequest request = new ReissueTokenRequest(TEST_REFRESH_TOKEN);

            given(refreshTokenRepository.findByJti(TEST_JTI)).willReturn(Optional.of(activeToken));
            // 원자적 업데이트가 0을 반환 (이미 다른 요청에서 rotate됨)
            given(refreshTokenRepository.markAsRotatedIfActive(eq(1L), any(LocalDateTime.class), eq(TEST_USER_IP)))
                    .willReturn(0);

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(request, httpRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(TokenErrorStatus._ALREADY_USED_REFRESH_TOKEN);
                    });

            // 새 토큰이 생성되지 않았는지 확인
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }
}
