package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardChartsResponse(
        ChartDataWithComparison monthlySignups,
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

    /**
     * 비교 데이터를 포함한 차트 데이터
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChartDataWithComparison(
            List<String> labels,
            List<Long> data,
            List<Long> previousData,
            List<String> previousLabels
    ) {
        public static ChartDataWithComparison of(List<String> labels, List<Long> data,
                                                  List<Long> previousData, List<String> previousLabels) {
            return new ChartDataWithComparison(labels, data, previousData, previousLabels);
        }

        public static ChartDataWithComparison of(List<String> labels, List<Long> data) {
            return new ChartDataWithComparison(labels, data, null, null);
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
            ChartDataWithComparison monthlySignups,
            ChartData providers,
            ChartData weekdayDistribution,
            List<KeywordItem> topKeywords
    ) {
        return new DashboardChartsResponse(monthlySignups, providers, weekdayDistribution, topKeywords);
    }
}
