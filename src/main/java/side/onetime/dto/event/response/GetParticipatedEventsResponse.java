package side.onetime.dto.event.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetParticipatedEventsResponse(
        List<GetParticipatedEventResponse> events,
        PageCursorInfo<String> pageCursorInfo
) {
    public static GetParticipatedEventsResponse of(List<GetParticipatedEventResponse> events, PageCursorInfo<String> pageCursorInfo) {
        return new GetParticipatedEventsResponse(events, pageCursorInfo);
    }
}
