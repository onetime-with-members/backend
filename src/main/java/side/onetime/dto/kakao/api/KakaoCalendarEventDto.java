package side.onetime.dto.kakao.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.util.List;

/**
 * 카카오 톡캘린더 이벤트 생성 요청 시 'event' 파라미터에 들어갈 데이터 구조입니다.
 */
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoCalendarEventDto(
        String title,
        KakaoCalendarTimeDto time,
        String description,
        List<Integer> reminders,
        String color,
        String rrule
) {
    @Builder
    public KakaoCalendarEventDto {
        if (description == null) {
            description = "OneTime에 의해 추가된 일정입니다.";
        }
        if (reminders == null) {
            reminders = List.of(30, 1440);
        }
        if (color == null) {
            color = "LAVENDER";
        }
    }

    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KakaoCalendarTimeDto(
            String startAt,
            String endAt,
            String timeZone
    ) {
        @Builder
        public KakaoCalendarTimeDto {
            if (timeZone == null) {
                timeZone = "Asia/Seoul";
            }
        }
    }
}
