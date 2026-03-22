package side.onetime.dto.admin.statistics.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long totalEvents,
        long confirmedEvents,
        double confirmationRate,
        long mau,
        double avgParticipantsPerEvent,
        double dormantRate,
        long marketingTargetUsers,
        LocalDateTime generatedAt,
        ComparisonData comparison
) {
    /**
     * 이전 기간 대비 증감률 데이터
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ComparisonData(
            Double totalUsersChange,
            Double totalEventsChange,
            Double mauChange,
            Double avgParticipantsChange,
            Double dormantRateChange,
            Double marketingTargetChange,
            long prevTotalUsers,
            long prevTotalEvents,
            long prevMau
    ) {
        public static ComparisonData of(
                long currUsers, long prevUsers,
                long currEvents, long prevEvents,
                long currMau, long prevMau,
                double currAvgParticipants, double prevAvgParticipants,
                double currDormantRate, double prevDormantRate,
                long currMarketing, long prevMarketing
        ) {
            return new ComparisonData(
                    calculateChangeRate(currUsers, prevUsers),
                    calculateChangeRate(currEvents, prevEvents),
                    calculateChangeRate(currMau, prevMau),
                    calculateChangeRate(currAvgParticipants, prevAvgParticipants),
                    calculatePointChange(currDormantRate, prevDormantRate),
                    calculateChangeRate(currMarketing, prevMarketing),
                    prevUsers,
                    prevEvents,
                    prevMau
            );
        }

        private static Double calculateChangeRate(long current, long previous) {
            if (previous == 0) return current > 0 ? 100.0 : null;
            return Math.round((double) (current - previous) / previous * 1000.0) / 10.0;
        }

        private static Double calculateChangeRate(double current, double previous) {
            if (previous == 0) return current > 0 ? 100.0 : null;
            return Math.round((current - previous) / previous * 1000.0) / 10.0;
        }

        private static Double calculatePointChange(double current, double previous) {
            // 포인트 변화 (예: 15% -> 20% = +5.0p)
            return Math.round((current - previous) * 10.0) / 10.0;
        }
    }

    public static DashboardSummaryResponse of(
            long totalUsers,
            long activeUsers,
            long totalEvents,
            long confirmedEvents,
            long mau,
            double avgParticipantsPerEvent,
            double dormantRate,
            long marketingTargetUsers
    ) {
        double confirmationRate = totalEvents > 0
                ? Math.round((double) confirmedEvents / totalEvents * 1000.0) / 10.0
                : 0.0;
        return new DashboardSummaryResponse(
                totalUsers,
                activeUsers,
                totalEvents,
                confirmedEvents,
                confirmationRate,
                mau,
                avgParticipantsPerEvent,
                dormantRate,
                marketingTargetUsers,
                LocalDateTime.now(),
                null
        );
    }

    public static DashboardSummaryResponse of(
            long totalUsers,
            long activeUsers,
            long totalEvents,
            long confirmedEvents,
            long mau,
            double avgParticipantsPerEvent,
            double dormantRate,
            long marketingTargetUsers,
            ComparisonData comparison
    ) {
        double confirmationRate = totalEvents > 0
                ? Math.round((double) confirmedEvents / totalEvents * 1000.0) / 10.0
                : 0.0;
        return new DashboardSummaryResponse(
                totalUsers,
                activeUsers,
                totalEvents,
                confirmedEvents,
                confirmationRate,
                mau,
                avgParticipantsPerEvent,
                dormantRate,
                marketingTargetUsers,
                LocalDateTime.now(),
                comparison
        );
    }
}
