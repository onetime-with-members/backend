package side.onetime.dto.fixed.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetFixedScheduleResponse(
        List<FixedScheduleResponse> schedules
) {
        public static GetFixedScheduleResponse from(List<FixedScheduleResponse> schedules) {
                return new GetFixedScheduleResponse(schedules);
        }
}
