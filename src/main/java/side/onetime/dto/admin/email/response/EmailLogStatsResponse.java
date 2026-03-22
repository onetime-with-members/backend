package side.onetime.dto.admin.email.response;

public record EmailLogStatsResponse(
        long totalSent,
        long sentToday,
        long failedToday,
        double successRate
) {
    public static EmailLogStatsResponse of(long totalSent, long sentToday, long failedToday) {
        double successRate = sentToday + failedToday > 0
                ? (double) sentToday / (sentToday + failedToday) * 100
                : 0.0;
        return new EmailLogStatsResponse(totalSent, sentToday, failedToday, Math.round(successRate * 10) / 10.0);
    }
}
