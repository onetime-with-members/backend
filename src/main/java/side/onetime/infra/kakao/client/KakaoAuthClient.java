package side.onetime.infra.kakao.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import side.onetime.dto.kakao.response.KakaoTokenResponse;

@FeignClient(
    name = "kakaoAuthClient",
    url = "https://kauth.kakao.com"
)
public interface KakaoAuthClient {

    /**
     * 카카오 OAuth 토큰을 발급받습니다.
     *
     * @param grantType 권한 부여 방식 (이 메서드에서는 "authorization_code")
     * @param clientId 카카오 앱의 REST API 키
     * @param redirectUri 카카오 로그인 시 설정했던 Redirect URI
     * @param code 사용자로부터 받은 인가 코드
     * @param clientSecret 카카오 앱의 Client Secret
     * @return 액세스 토큰 및 리프레시 토큰 정보가 담긴 응답 DTO
     */
    @PostMapping(
        value = "/oauth/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KakaoTokenResponse getAccessToken(
        @RequestParam("grant_type") String grantType,
        @RequestParam("client_id") String clientId,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam("code") String code,
        @RequestParam("client_secret") String clientSecret
    );
}
