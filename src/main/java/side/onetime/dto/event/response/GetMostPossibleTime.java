package side.onetime.dto.event.response;

import static side.onetime.util.DateUtil.*;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Schedule;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetMostPossibleTime(
        String timePoint,
        String startTime,
        String endTime,
        int possibleCount,
        List<String> possibleNames,
        List<String> impossibleNames
) {
    public static GetMostPossibleTime dayOf(Schedule schedule, List<String> possibleNames, List<String> impossibleNames) {
        return new GetMostPossibleTime(
                schedule.getDay(),
                schedule.getTime(),
                addThirtyMinutes(schedule.getTime()),
                possibleNames.size(),
                possibleNames,
                impossibleNames
        );
    }

    public static GetMostPossibleTime dateOf(Schedule schedule, List<String> possibleNames, List<String> impossibleNames) {
        return new GetMostPossibleTime(
                schedule.getDate(),
                schedule.getTime(),
                addThirtyMinutes(schedule.getTime()),
                possibleNames.size(),
                possibleNames,
                impossibleNames
        );
    }

    public GetMostPossibleTime updateEndTime(String endTime) {
        return new GetMostPossibleTime(
                this.timePoint,
                this.startTime,
                addThirtyMinutes(endTime),
                this.possibleCount,
                this.possibleNames,
                this.impossibleNames
        );
    }
}
