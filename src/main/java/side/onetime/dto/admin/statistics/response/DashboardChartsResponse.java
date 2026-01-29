package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardChartsResponse(
        ChartData monthlySignups,
        ChartData providers,
        ChartData weekdayDistribution,
        List<KeywordItem> topKeywords
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChartData(
            List<String> labels,
            List<Long> data
    ) {
        public static ChartData of(List<String> labels, List<Long> data) {
            return new ChartData(labels, data);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KeywordItem(
            String keyword,
            long count,
            double percentage
    ) {
        public static KeywordItem of(String keyword, long count, double percentage) {
            return new KeywordItem(keyword, count, percentage);
        }
    }

    public static DashboardChartsResponse of(
            ChartData monthlySignups,
            ChartData providers,
            ChartData weekdayDistribution,
            List<KeywordItem> topKeywords
    ) {
        return new DashboardChartsResponse(monthlySignups, providers, weekdayDistribution, topKeywords);
    }
}
