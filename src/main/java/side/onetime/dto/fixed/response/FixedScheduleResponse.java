package side.onetime.dto.fixed.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
