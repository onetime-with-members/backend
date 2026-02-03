package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RetentionStatisticsResponse(
        List<MonthlyMau> monthlyMau,
        long dormantUsers7Days,
        long dormantUsers30Days,
        long dormantUsers90Days,
        double returningUserRate,
        double avgDaysToFirstEvent
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyMau(
            String month,
            long count
    ) {}

    public static RetentionStatisticsResponse of(
            List<MonthlyMau> monthlyMau,
            long dormantUsers7Days,
            long dormantUsers30Days,
            long dormantUsers90Days,
            double returningUserRate,
            double avgDaysToFirstEvent
    ) {
        return new RetentionStatisticsResponse(
                monthlyMau, dormantUsers7Days, dormantUsers30Days,
                dormantUsers90Days, returningUserRate, avgDaysToFirstEvent
        );
    }
}
