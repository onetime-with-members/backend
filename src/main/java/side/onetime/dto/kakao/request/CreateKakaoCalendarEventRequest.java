package side.onetime.dto.kakao.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateKakaoCalendarEventRequest(
        @NotBlank(message = "액세스 토큰은 필수입니다.")
        String accessToken,
        @NotNull(message = "이벤트 ID는 필수입니다.")
        UUID eventId,
        String description,
        List<Integer> reminders,
        String color,
        String rrule,
        String timeZone
) {
}
