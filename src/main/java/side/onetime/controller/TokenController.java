package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.TokenService;
import side.onetime.util.ClientInfoExtractor;

@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final ClientInfoExtractor clientInfoExtractor;

    /**
     * 액세스 토큰 재발행 API.
     *
     * @param reissueAccessTokenRequest 리프레쉬 토큰을 포함한 요청 객체
     * @param httpRequest HttpServletRequest (IP, User-Agent 추출용)
     * @return 재발행된 액세스 토큰과 리프레쉬 토큰을 포함하는 응답 객체
     */
    @PostMapping("/action-reissue")
    public ResponseEntity<ApiResponse<ReissueTokenResponse>> reissueToken(
            @Valid @RequestBody ReissueTokenRequest reissueAccessTokenRequest,
            HttpServletRequest httpRequest) {

        String userIp = clientInfoExtractor.extractClientIp(httpRequest);
        String userAgent = clientInfoExtractor.extractUserAgent(httpRequest);
        ReissueTokenResponse reissueTokenResponse = tokenService.reissueToken(reissueAccessTokenRequest, userIp, userAgent);
        return ApiResponse.onSuccess(SuccessStatus._REISSUE_TOKENS, reissueTokenResponse);
    }
}
