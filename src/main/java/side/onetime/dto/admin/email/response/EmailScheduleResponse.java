package side.onetime.dto.admin.email.response;

import java.time.LocalDateTime;

import side.onetime.domain.EmailSchedule;
import side.onetime.domain.enums.EmailScheduleStatus;

public record EmailScheduleResponse(
        Long id,
        Long templateId,
        String templateName,
        String targetGroup,
        Integer targetLimit,
        LocalDateTime scheduledAt,
        EmailScheduleStatus status,
        Integer sentCount,
        Integer failedCount,
        LocalDateTime createdAt
) {
    public static EmailScheduleResponse from(EmailSchedule schedule) {
        return new EmailScheduleResponse(
                schedule.getId(),
                schedule.getTemplate() != null ? schedule.getTemplate().getId() : null,
                schedule.getTemplate() != null ? schedule.getTemplate().getName() : null,
                schedule.getTargetGroup(),
                schedule.getTargetLimit(),
                schedule.getScheduledAt(),
                schedule.getStatus(),
                schedule.getSentCount(),
                schedule.getFailedCount(),
                schedule.getCreatedAt()
        );
    }
}
