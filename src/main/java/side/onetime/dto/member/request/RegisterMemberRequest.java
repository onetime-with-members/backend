package side.onetime.dto.member.request;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import side.onetime.domain.Event;
import side.onetime.domain.Member;
import side.onetime.dto.member.response.ScheduleResponse;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterMemberRequest(
        @NotBlank(message = "Event ID는 필수 값입니다.") String eventId,
        @NotBlank(message = "이름은 필수 값입니다.") String name,
        @NotBlank(message = "PIN은 필수 값입니다.") String pin,
        @NotNull(message = "스케줄 목록은 필수 값입니다.") List<ScheduleResponse> schedules
) {
    public Member toEntity(Event event) {
        return Member.builder()
                .event(event)
                .memberId(UUID.randomUUID())
                .name(name)
                .pin(pin)
                .build();
    }
}
