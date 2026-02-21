package side.onetime.dto.schedule.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Selection;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DateSchedule(
        @JsonProperty("time_point") String date,
        List<String> times
) {
    public static DateSchedule from(List<Selection> selections) {
        List<String> times = new ArrayList<>();
        for (Selection selection : selections) {
            times.add(selection.getSchedule().getTime());
        }
        return new DateSchedule(selections.get(0).getSchedule().getDate(), times);
    }
}
