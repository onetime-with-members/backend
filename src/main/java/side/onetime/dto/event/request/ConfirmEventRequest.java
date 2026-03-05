package side.onetime.dto.event.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConfirmEventRequest(
        String startDate,
        String endDate,
        String startDay,
        String endDay,
        @NotBlank(message = "시작 시간은 필수 값입니다.")
        String startTime,
        @NotBlank(message = "종료 시간은 필수 값입니다.")
        String endTime
) {
}
