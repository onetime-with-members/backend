package side.onetime.dto.schedule.request;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetFilteredSchedulesRequest(
        List<Long> users,
        List<Long> members
) {
}
