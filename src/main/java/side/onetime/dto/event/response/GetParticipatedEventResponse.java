package side.onetime.dto.event.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.Event;
import side.onetime.domain.EventConfirmation;
import side.onetime.domain.EventParticipation;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.EventStatus;
import side.onetime.domain.enums.ParticipationRole;

import java.util.List;
import java.util.UUID;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetParticipatedEventResponse(
        UUID eventId,
        Category category,
        String title,
        String createdDate,
        int participantCount,
        EventStatus eventStatus,
        ParticipationRole participationRole,
        List<GetMostPossibleTime> mostPossibleTimes,
        ConfirmationDto confirmation
) {
    public static GetParticipatedEventResponse of(Event event, EventParticipation eventParticipation, int participantCount, List<GetMostPossibleTime> mostPossibleTimes, EventConfirmation confirmation) {
        return new GetParticipatedEventResponse(
                event.getEventId(),
                event.getCategory(),
                event.getTitle(),
                String.valueOf(event.getCreatedDate()),
                participantCount,
                event.getStatus(),
                eventParticipation.getParticipationRole(),
                mostPossibleTimes,
                confirmation != null ? ConfirmationDto.from(confirmation) : null
        );
    }
}
