package side.onetime.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import side.onetime.auth.annotation.PublicApi;
import side.onetime.dto.kakao.api.KakaoCalendarEventResponse;
import side.onetime.dto.kakao.request.CreateKakaoCalendarEventRequest;
import side.onetime.dto.kakao.request.KakaoTokenRequest;
import side.onetime.dto.kakao.response.KakaoTokenResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.KakaoService;

@RestController
@RequestMapping("/api/v1/kakao")
@RequiredArgsConstructor
public class KakaoController {

    private final KakaoService kakaoService;

    /**
     * 카카오 인증 페이지 URL 조회 API.
     *
     * 프론트엔드에서 카카오 로그인을 통해 'talk_calendar' 권한을 요청할 수 있는 인가 코드 발급용 URL로 리다이렉트합니다.
     *
     * @return 카카오 OAuth 인증 페이지로의 RedirectView
     */
    @PublicApi
    @GetMapping("/authorize-url")
    public RedirectView getAuthorizeUrl() {
        return new RedirectView(kakaoService.getAuthorizeUrl());
    }

    /**
     * 카카오 액세스 토큰 발급 API.
     *
     * 전달받은 인가 코드(code)를 사용하여 카카오로부터 액세스 토큰을 발급받습니다.
     * 발급받은 토큰은 이후 톡캘린더 API 호출 시 사용됩니다.
     *
     * @param request 인가 코드가 포함된 요청 객체
     * @return 발급된 카카오 토큰 정보 (accessToken)
     */
    @PublicApi
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<KakaoTokenResponse>> getKakaoToken(@RequestBody @Valid KakaoTokenRequest request) {
        KakaoTokenResponse response = kakaoService.getKakaoToken(request.code());
        return ApiResponse.onSuccess(SuccessStatus._CREATE_KAKAO_TOKEN, response);
    }

    /**
     * 카카오 톡캘린더 일정 생성 API.
     *
     * 확정된 이벤트 정보를 바탕으로 사용자의 카카오 톡캘린더에 새로운 일정을 생성합니다.
     * 이 API를 호출하기 위해서는 이전에 발급받은 카카오 액세스 토큰이 필요합니다.
     *
     * @param request 액세스 토큰과 서비스 내 이벤트 ID가 포함된 요청 객체
     * @return 생성된 카카오 캘린더 이벤트의 ID 정보
     */
    @PublicApi
    @PostMapping("/calendar/confirmation")
    public ResponseEntity<ApiResponse<KakaoCalendarEventResponse>> createCalendarEvent(
            @RequestBody @Valid CreateKakaoCalendarEventRequest request
    ) {
        KakaoCalendarEventResponse response = kakaoService.createCalendarEvent(request);
        return ApiResponse.onSuccess(SuccessStatus._CREATE_KAKAO_CALENDAR_EVENT, response);
    }
}
