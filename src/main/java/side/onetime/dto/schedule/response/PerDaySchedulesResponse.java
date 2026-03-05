package side.onetime.dto.schedule.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PerDaySchedulesResponse(
        String name,
        @JsonProperty("schedules") List<DaySchedule> daySchedules
) {
    public static PerDaySchedulesResponse of(String name, List<DaySchedule> daySchedules) {
        return new PerDaySchedulesResponse(name, daySchedules);
    }
}
