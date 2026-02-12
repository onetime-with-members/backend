package side.onetime.dto.admin.email.response;

public record SendEmailResponse(
        boolean success,
        int queuedCount,
        String message
) {
    public static SendEmailResponse queued(int queuedCount) {
        return new SendEmailResponse(true, queuedCount,
                queuedCount + "건의 이메일이 발송 대기열에 등록되었습니다.");
    }

    public static SendEmailResponse empty() {
        return new SendEmailResponse(true, 0, "발송 대상이 없습니다.");
    }
}
