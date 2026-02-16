package side.onetime.dto.event.response;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Event;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateEventResponse(
        UUID eventId
) {
    public static CreateEventResponse of(Event event) {
        return new CreateEventResponse(event.getEventId());
    }
}
