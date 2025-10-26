package side.onetime.dto.event.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModifyEventRequest(
        @NotBlank(message = "제목은 필수 값입니다.") String title,
        @NotBlank(message = "시작 시간은 필수 값입니다.") String startTime,
        @NotBlank(message = "종료 시간은 필수 값입니다.") String endTime,
        @NotNull(message = "설문 범위는 필수 값입니다.") List<String> ranges
) {
}
