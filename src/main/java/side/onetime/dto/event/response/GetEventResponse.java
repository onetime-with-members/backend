package side.onetime.dto.event.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Event;
import side.onetime.domain.EventConfirmation;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.EventStatus;
import side.onetime.domain.enums.ParticipationRole;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetEventResponse(
        String eventId,
        String title,
        String startTime,
        String endTime,
        Category category,
        List<String> ranges,
        EventStatus eventStatus,
        ParticipationRole participationRole,
        ConfirmationDto confirmation
) {
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConfirmationDto(
            String startDate,
            String endDate,
            String startDay,
            String endDay,
            String startTime,
            String endTime,
            LocalDateTime createdDate
    ) {
        public static ConfirmationDto from(EventConfirmation confirmation) {
            return new ConfirmationDto(
                    confirmation.getStartDate(),
                    confirmation.getEndDate(),
                    confirmation.getStartDay(),
                    confirmation.getEndDay(),
                    confirmation.getStartTime(),
                    confirmation.getEndTime(),
                    confirmation.getCreatedDate()
            );
        }
    }

    public static GetEventResponse of(Event event, List<String> ranges, ParticipationRole participationRole, EventConfirmation eventConfirmation) {
        return new GetEventResponse(
                String.valueOf(event.getEventId()),
                event.getTitle(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCategory(),
                ranges,
                event.getStatus(),
                participationRole,
                eventConfirmation != null ? ConfirmationDto.from(eventConfirmation) : null
        );
    }
}
