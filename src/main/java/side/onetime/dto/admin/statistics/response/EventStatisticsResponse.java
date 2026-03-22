package side.onetime.dto.admin.statistics.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventStatisticsResponse(
        long totalEvents,
        long activeEvents,
        long confirmedEvents,
        double confirmationRate,
        double avgConfirmationHours,
        double avgParticipants,
        Map<String, Long> categoryDistribution,
        Map<String, Long> weekdayDistribution,
        List<MonthlyData> monthlyEvents,
        List<KeywordData> topKeywords
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyData(
            String month,
            long count
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KeywordData(
            String keyword,
            long count
    ) {}

    public static EventStatisticsResponse of(
            long totalEvents,
            long activeEvents,
            long confirmedEvents,
            double avgConfirmationHours,
            double avgParticipants,
            Map<String, Long> categoryDistribution,
            Map<String, Long> weekdayDistribution,
            List<MonthlyData> monthlyEvents,
            List<KeywordData> topKeywords
    ) {
        double confirmationRate = totalEvents > 0
                ? Math.round((double) confirmedEvents / totalEvents * 1000.0) / 10.0
                : 0.0;
        return new EventStatisticsResponse(
                totalEvents, activeEvents, confirmedEvents, confirmationRate,
                Math.round(avgConfirmationHours * 10.0) / 10.0,
                avgParticipants,
                categoryDistribution, weekdayDistribution,
                monthlyEvents, topKeywords
        );
    }
}
