package side.onetime.dto.schedule.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PerDateSchedulesResponse(
        String name,
        @JsonProperty("schedules") List<DateSchedule> dateSchedules
) {
    public static PerDateSchedulesResponse of(String name, List<DateSchedule> dateSchedules) {
        return new PerDateSchedulesResponse(name, dateSchedules);
    }
}
