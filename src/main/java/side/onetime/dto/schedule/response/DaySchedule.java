package side.onetime.dto.schedule.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Selection;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DaySchedule(
        @JsonProperty("time_point") String day,
        List<String> times
) {
    public static DaySchedule from(List<Selection> selections) {
        List<String> times = new ArrayList<>();
        for (Selection selection : selections) {
            times.add(selection.getSchedule().getTime());
        }
        return new DaySchedule(selections.get(0).getSchedule().getDay(), times);
    }
}
