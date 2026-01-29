package side.onetime.dto.admin.statistics.response;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CohortRetentionResponse(
        List<CohortRow> cohorts,
        List<String> periods
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CohortRow(
            String month,
            int size,
            List<Double> retention
    ) {
        public static CohortRow of(String month, int size, List<Double> retention) {
            return new CohortRow(month, size, retention);
        }
    }

    public static CohortRetentionResponse of(List<CohortRow> cohorts, int maxPeriods) {
        List<String> periods = IntStream.range(0, maxPeriods)
                .mapToObj(i -> "M" + i)
                .toList();
        return new CohortRetentionResponse(cohorts, periods);
    }
}
