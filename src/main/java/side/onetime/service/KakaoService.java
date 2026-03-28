package side.onetime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import side.onetime.domain.Event;
import side.onetime.domain.EventConfirmation;
import side.onetime.domain.enums.Category;
import side.onetime.dto.kakao.api.KakaoCalendarEventDto;
import side.onetime.dto.kakao.api.KakaoCalendarEventResponse;
import side.onetime.dto.kakao.request.CreateKakaoCalendarEventRequest;
import side.onetime.dto.kakao.response.KakaoTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EventErrorStatus;
import side.onetime.infra.kakao.client.KakaoApiClient;
import side.onetime.infra.kakao.client.KakaoAuthClient;
import side.onetime.repository.EventConfirmationRepository;
import side.onetime.repository.EventRepository;
import side.onetime.util.DateUtil;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final ObjectMapper objectMapper;
    private final EventRepository eventRepository;
    private final EventConfirmationRepository eventConfirmationRepository;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${app.kakao.calendar-redirect-uri}")
    private String calendarRedirectUri;

    /**
     * 카카오 인증 페이지 리다이렉트 메서드.
     *
     * 프론트엔드에서 카카오 로그인을 통해 'talk_calendar' 권한을 요청할 수 있는 인가 코드 발급용 URL입니다.
     *
     * @return 카카오 OAuth 인증 페이지 URL
     */
    public String getAuthorizeUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", calendarRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "talk_calendar")
                .build()
                .toUriString();
    }

    /**
     * 카카오 액세스 토큰 발급 메서드.
     *
     * 전달받은 인가 코드를 사용하여 카카오로부터 액세스 토큰 정보를 가져옵니다.
     *
     * @param code 카카오로부터 전달받은 인가 코드
     * @return 카카오 토큰 응답 (accessToken)
     */
    public KakaoTokenResponse getKakaoToken(String code) {
        return kakaoAuthClient.getAccessToken(
                "authorization_code",
                clientId,
                calendarRedirectUri,
                code,
                clientSecret
        );
    }

    /**
     * 카카오 톡캘린더에 일정을 생성하는 메서드.
     *
     * 확정한 이벤트 정보(제목, 확정 시간 등)를 바탕으로 사용자의 카카오 캘린더에 일정을 등록합니다.
     * 카카오 API 호출을 위해 유효한 액세스 토큰이 필요합니다.
     *
     * @param request     톡캘린더에 등록할 이벤트 요청 객체
     * @return 생성된 일정 정보 (event_id 등)
     * @throws CustomException 이벤트를 찾을 수 없거나, 확정 정보가 없거나, JSON 직렬화에 실패한 경우
     */
    public KakaoCalendarEventResponse createCalendarEvent(CreateKakaoCalendarEventRequest request) {
        Event event = eventRepository.findByEventId(request.eventId())
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        EventConfirmation confirmation = eventConfirmationRepository.findByEventId(event.getId())
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT_CONFIRMATION));

        KakaoCalendarEventDto eventDto = buildKakaoCalendarEventDto(event, confirmation, request);

        try {
            String eventJson = objectMapper.writeValueAsString(eventDto);
            return kakaoApiClient.createTalkCalendarEvent("Bearer " + request.accessToken(), eventJson);
        } catch (JsonProcessingException e) {
            throw new CustomException(EventErrorStatus._FAILED_SERIALIZE_KAKAO_EVENT);
        }
    }

    /**
     * 카카오 캘린더 API 요청을 위한 DTO를 구성합니다.
     * 
     * 확정된 이벤트의 카테고리(날짜/요일)에 따라 시작 시간과 종료 시간을 포맷팅하고,
     * 요일 기반 이벤트의 경우 반복 설정(RRULE)을 추가합니다.
     *
     * @param event        이벤트 엔티티
     * @param confirmation 이벤트 확정 정보 엔티티
     * @return 카카오 캘린더 API 규격에 맞춘 DTO
     */
    private KakaoCalendarEventDto buildKakaoCalendarEventDto(Event event, EventConfirmation confirmation, CreateKakaoCalendarEventRequest request) {
        Category category = event.getCategory();

        String startAt = DateUtil.formatToIsoDateTime(category,
                (category == Category.DATE) ? confirmation.getStartDate() : confirmation.getStartDay(),
                confirmation.getStartTime());

        String endAt = DateUtil.formatToIsoDateTime(category,
                (category == Category.DATE) ? confirmation.getEndDate() : confirmation.getEndDay(),
                confirmation.getEndTime());

        String rrule = (request.rrule() != null) ? request.rrule() :
                ((category == Category.DAY) ? "FREQ=WEEKLY" : null);

        return KakaoCalendarEventDto.builder()
                .title(event.getTitle())
                .time(KakaoCalendarEventDto.KakaoCalendarTimeDto.builder()
                        .startAt(startAt)
                        .endAt(endAt)
                        .timeZone(request.timeZone())
                        .build())
                .description(request.description())
                .reminders(request.reminders())
                .color(request.color())
                .rrule(rrule)
                .build();
    }
}
