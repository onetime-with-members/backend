package side.onetime.dto.event.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetAllUserParticipatedEventsResponse(
        List<GetUserParticipatedEventResponse> events,
        PageCursorInfo<String> pageCursorInfo
) {
    public static GetAllUserParticipatedEventsResponse of(List<GetUserParticipatedEventResponse> events, PageCursorInfo<String> pageCursorInfo) {
        return new GetAllUserParticipatedEventsResponse(events, pageCursorInfo);
    }
}
