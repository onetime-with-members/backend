package side.onetime.dto.admin.statistics.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 이벤트 확정 통계 응답 DTO
 * 확정자 유형 분포, 카테고리별 확정률, 일별 확정 추이
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventConfirmationStatsResponse(
        long confirmedEvents,
        long totalEvents,
        double confirmationRate,
        double avgConfirmationHours,
        Map<String, Long> confirmerRoleDistribution,
        List<CategoryConfirmationRate> categoryConfirmationRates,
        List<DailyConfirmationTrend> dailyTrend
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CategoryConfirmationRate(
            String category,
            long totalCount,
            long confirmedCount,
            double rate
    ) {
        public static CategoryConfirmationRate of(String category, long total, long confirmed) {
            double rate = total > 0 ? Math.round((double) confirmed / total * 1000.0) / 10.0 : 0.0;
            return new CategoryConfirmationRate(category, total, confirmed, rate);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DailyConfirmationTrend(
            String date,
            long confirmedCount,
            long createdCount
    ) {}

    public static EventConfirmationStatsResponse of(
            long confirmedEvents,
            long totalEvents,
            double avgConfirmationHours,
            Map<String, Long> confirmerRoleDistribution,
            List<CategoryConfirmationRate> categoryConfirmationRates,
            List<DailyConfirmationTrend> dailyTrend
    ) {
        double confirmationRate = totalEvents > 0
                ? Math.round((double) confirmedEvents / totalEvents * 1000.0) / 10.0
                : 0.0;
        return new EventConfirmationStatsResponse(
                confirmedEvents, totalEvents, confirmationRate,
                Math.round(avgConfirmationHours * 10.0) / 10.0,
                confirmerRoleDistribution, categoryConfirmationRates, dailyTrend
        );
    }
}
