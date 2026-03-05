package side.onetime.dto.user.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateUserSleepTimeRequest(
        @NotBlank(message = "수면 시작 시간은 필수 값입니다.")
        String sleepStartTime,
        @NotBlank(message = "수면 종료 시간은 필수 값입니다.")
        String sleepEndTime
) {
}
