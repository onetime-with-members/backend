package side.onetime.dto.admin.email.request;

import java.time.LocalDateTime;
import java.util.List;

public record EmailEventMessage(
        String subject,
        String content,
        String contentType,
        String targetGroup,
        List<Recipient> recipients,
        String requestedAt
) {
    public record Recipient(
            Long emailLogId,
            String email,
            Long userId,
            String name,
            String nickname
    ) {
    }

    public static EmailEventMessage of(String subject, String content, String contentType,
                                        String targetGroup, List<Recipient> recipients) {
        return new EmailEventMessage(
                subject, content, contentType, targetGroup,
                recipients, LocalDateTime.now().toString()
        );
    }
}
