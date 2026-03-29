package side.onetime.infra.kakao.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import side.onetime.dto.kakao.api.KakaoCalendarEventResponse;

@FeignClient(
    name = "kakaoApiClient",
    url = "https://kapi.kakao.com"
)
public interface KakaoApiClient {

    /**
     * 카카오 톡캘린더 이벤트를 생성합니다.
     *
     * @param accessToken "Bearer {token}" 형식의 카카오 액세스 토큰
     * @param eventJson JSON 문자열로 직렬화된 이벤트 데이터 (KakaoCalendarEventDto 규격)
     * @return 생성된 이벤트의 식별 정보 (event_id)
     */
    @PostMapping(
        value = "/v2/api/calendar/create/event",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KakaoCalendarEventResponse createTalkCalendarEvent(
        @RequestHeader("Authorization") String accessToken,
        @RequestParam("event") String eventJson
    );
}
