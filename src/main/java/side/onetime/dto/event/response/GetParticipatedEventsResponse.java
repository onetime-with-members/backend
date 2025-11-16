package side.onetime.dto.event.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetParticipatedEventsResponse(
        List<GetParticipatedEventResponse> events,
        PageCursorInfo<String> pageCursorInfo
) {
    public static GetParticipatedEventsResponse of(List<GetParticipatedEventResponse> events, PageCursorInfo<String> pageCursorInfo) {
        return new GetParticipatedEventsResponse(events, pageCursorInfo);
    }
}
