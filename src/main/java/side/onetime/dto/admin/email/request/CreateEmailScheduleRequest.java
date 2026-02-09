package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmailScheduleRequest(
        @NotNull(message = "템플릿 ID는 필수입니다.")
        Long templateId,

        @NotBlank(message = "대상 그룹은 필수입니다.")
        String targetGroup,

        Integer targetLimit,

        @NotBlank(message = "예약 시간은 필수입니다.")
        String scheduledAt
) {
    public int getTargetLimit() {
        return targetLimit != null ? targetLimit : 100;
    }
}
