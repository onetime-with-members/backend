package side.onetime.dto.event.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import side.onetime.domain.enums.SelectionSource;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConfirmEventRequest(
        String startDate,
        String endDate,
        String startDay,
        String endDay,
        @NotBlank(message = "시작 시간은 필수 값입니다.")
        String startTime,
        @NotBlank(message = "종료 시간은 필수 값입니다.")
        String endTime,
        @NotNull(message = "선택 방식은 필수 값입니다.")
        SelectionSource selectionSource
) {
}
