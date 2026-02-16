package side.onetime.dto.schedule.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import side.onetime.dto.schedule.response.DateSchedule;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateDateScheduleRequest(
        @NotBlank(message = "Event ID는 필수 값입니다.") String eventId,
        String memberId,
        @JsonProperty("schedules") @NotNull(message = "스케줄 목록은 필수 값입니다.") List<DateSchedule> dateSchedules
) {
}
