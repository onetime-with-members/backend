package side.onetime.dto.member.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginMemberRequest(
        @NotBlank(message = "Event ID는 필수 값입니다.") String eventId,
        @NotBlank(message = "이름은 필수 값입니다.") String name,
        @NotBlank(message = "PIN은 필수 값입니다.") String pin
) {
}
