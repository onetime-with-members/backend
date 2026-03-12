package side.onetime.dto.event.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.EventConfirmation;

import java.time.LocalDateTime;

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
