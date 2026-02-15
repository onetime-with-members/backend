package side.onetime.dto.event.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.enums.EventStatus;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConfirmEventResponse(
        UUID eventId,
        EventStatus eventStatus,
        LocalDateTime createdDate
) {
    public static ConfirmEventResponse of(UUID eventId, EventStatus eventStatus, LocalDateTime createdDate) {
        return new ConfirmEventResponse(eventId, eventStatus, createdDate);
    }
}
