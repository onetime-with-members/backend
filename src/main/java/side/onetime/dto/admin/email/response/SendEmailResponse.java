package side.onetime.dto.admin.email.response;

import java.util.List;

public record SendEmailResponse(
        boolean success,
        int sentCount,
        int failedCount,
        List<String> failedEmails
) {
    public static SendEmailResponse of(int sentCount, int failedCount, List<String> failedEmails) {
        return new SendEmailResponse(
                failedCount == 0,
                sentCount,
                failedCount,
                failedEmails
        );
    }

    public static SendEmailResponse success(int sentCount) {
        return new SendEmailResponse(true, sentCount, 0, List.of());
    }

    public static SendEmailResponse failure(List<String> failedEmails) {
        return new SendEmailResponse(false, 0, failedEmails.size(), failedEmails);
    }
}
