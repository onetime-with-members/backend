package side.onetime.dto.admin.email.response;

import java.time.LocalDateTime;
import java.util.List;

import side.onetime.domain.EmailLog;
import side.onetime.domain.enums.EmailLogStatus;

public record EmailLogResponse(
        Long id,
        String recipient,
        String subject,
        String contentType,
        EmailLogStatus status,
        String errorMessage,
        String targetGroup,
        LocalDateTime sentAt
) {
    public static EmailLogResponse from(EmailLog emailLog) {
        return new EmailLogResponse(
                emailLog.getId(),
                emailLog.getRecipient(),
                emailLog.getSubject(),
                emailLog.getContentType(),
                emailLog.getStatus(),
                emailLog.getErrorMessage(),
                emailLog.getTargetGroup(),
                emailLog.getSentAt()
        );
    }

    public static List<EmailLogResponse> fromList(List<EmailLog> emailLogs) {
        return emailLogs.stream()
                .map(EmailLogResponse::from)
                .toList();
    }
}
