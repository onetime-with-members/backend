package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FunnelAnalysisResponse(
        List<FunnelStep> steps,
        long totalSignups
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FunnelStep(
            String name,
            String label,
            long count,
            double rate,
            double dropoffRate
    ) {
        public static FunnelStep of(String name, String label, long count, double rate, double dropoffRate) {
            return new FunnelStep(name, label, count, rate, dropoffRate);
        }
    }

    public static FunnelAnalysisResponse of(List<FunnelStep> steps, long totalSignups) {
        return new FunnelAnalysisResponse(steps, totalSignups);
    }
}
