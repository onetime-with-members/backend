package side.onetime.dto.admin.statistics.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long totalEvents,
        long mau,
        double avgParticipantsPerEvent,
        double dormantRate,
        long marketingTargetUsers,
        LocalDateTime generatedAt
) {
    public static DashboardSummaryResponse of(
            long totalUsers,
            long activeUsers,
            long totalEvents,
            long mau,
            double avgParticipantsPerEvent,
            double dormantRate,
            long marketingTargetUsers
    ) {
        return new DashboardSummaryResponse(
                totalUsers,
                activeUsers,
                totalEvents,
                mau,
                avgParticipantsPerEvent,
                dormantRate,
                marketingTargetUsers,
                LocalDateTime.now()
        );
    }
}
