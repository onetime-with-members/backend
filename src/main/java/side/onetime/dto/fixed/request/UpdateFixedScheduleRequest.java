package side.onetime.dto.fixed.request;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;
import side.onetime.dto.fixed.response.FixedScheduleResponse;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateFixedScheduleRequest(
        @NotNull(message = "스케줄 목록은 필수 값입니다.")
        List<FixedScheduleResponse> schedules
) {
}
