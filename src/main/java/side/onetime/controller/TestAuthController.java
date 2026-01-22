package side.onetime.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.test.request.TestLoginRequest;
import side.onetime.dto.test.response.TestTokenResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.TestAuthService;

@RestController
@RequestMapping("/api/v1/test/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestAuthController {

    private final TestAuthService testAuthService;

    /**
     * 테스트 로그인 API.
     *
     * E2E 테스트 환경에서 소셜 로그인 없이 토큰을 발급합니다.
     * local, dev 환경에서만 동작하며 prod에서는 빈이 생성되지 않습니다.
     *
     * @param request 시크릿 키를 포함한 요청 객체
     * @return Access Token과 Refresh Token을 포함하는 응답 객체
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TestTokenResponse>> testLogin(
            @Valid @RequestBody TestLoginRequest request) {

        TestTokenResponse response = testAuthService.login(request);
        return ApiResponse.onSuccess(SuccessStatus._TEST_LOGIN, response);
    }

    /**
     * 만료된 토큰 발급 API.
     *
     * E2E 테스트 환경에서 401 처리, 토큰 재발급 플로우 테스트를 위해
     * 이미 만료된 액세스 토큰을 발급합니다.
     *
     * @param request 시크릿 키를 포함한 요청 객체
     * @return 만료된 Access Token을 포함하는 응답 객체
     */
    @PostMapping("/expired-token")
    public ResponseEntity<ApiResponse<TestTokenResponse>> getExpiredToken(
            @Valid @RequestBody TestLoginRequest request) {

        TestTokenResponse response = testAuthService.generateExpiredToken(request);
        return ApiResponse.onSuccess(SuccessStatus._TEST_EXPIRED_TOKEN, response);
    }
}
