package side.onetime.dto.fixed.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FixedScheduleResponse(
        String timePoint,
        List<String> times
) {
    public static FixedScheduleResponse of(String timePoint, List<String> times) {
        return new FixedScheduleResponse(
                timePoint,
                times);
    }
}
