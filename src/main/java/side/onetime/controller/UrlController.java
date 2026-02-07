package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.auth.annotation.PublicApi;
import side.onetime.dto.url.request.ConvertToOriginalUrlRequest;
import side.onetime.dto.url.request.ConvertToShortenUrlRequest;
import side.onetime.dto.url.response.ConvertToOriginalUrlResponse;
import side.onetime.dto.url.response.ConvertToShortenUrlResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.UrlService;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    /**
     * 원본 URL을 단축 URL로 변환하는 API.
     *
     * 이 API는 제공된 원본 URL을 단축 URL로 변환합니다.
     * 주어진 URL에서 이벤트 ID를 추출하고, 해당 이벤트가 존재할 경우에만 단축 URL을 생성하여 반환합니다.
     *
     * @param convertToShortenUrlRequest 원본 URL을 포함한 요청 객체
     * @return 변환된 단축 URL을 포함하는 응답 객체
     */
    @PublicApi
    @PostMapping("/action-shorten")
    public ResponseEntity<ApiResponse<ConvertToShortenUrlResponse>> convertToShortenUrl(
            @Valid @RequestBody ConvertToShortenUrlRequest convertToShortenUrlRequest) {

        ConvertToShortenUrlResponse convertToShortenUrlResponse = urlService.convertToShortenUrl(convertToShortenUrlRequest);
        return ApiResponse.onSuccess(SuccessStatus._CONVERT_TO_SHORTEN_URL, convertToShortenUrlResponse);
    }

    /**
     * 단축 URL을 원본 URL로 복원하는 API.
     *
     * 이 API는 단축된 URL을 원래의 URL로 복원합니다.
     * 복원된 URL에서 이벤트 ID를 추출하여, 해당 이벤트가 존재하는지 확인 후 원본 URL을 반환합니다.
     *
     * @param convertToOriginalUrlRequest 단축 URL을 포함한 요청 객체
     * @return 복원된 원본 URL을 포함하는 응답 객체
     */
    @PublicApi
    @PostMapping("/action-original")
    public ResponseEntity<ApiResponse<ConvertToOriginalUrlResponse>> convertToOriginalUrl(
            @Valid @RequestBody ConvertToOriginalUrlRequest convertToOriginalUrlRequest) {

        ConvertToOriginalUrlResponse convertToOriginalUrlResponse = urlService.convertToOriginalUrl(convertToOriginalUrlRequest);
        return ApiResponse.onSuccess(SuccessStatus._CONVERT_TO_ORIGINAL_URL, convertToOriginalUrlResponse);
    }
}
