package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Time to Value (TTV) Distribution Response
 * 가입 후 첫 이벤트 생성까지 걸린 시간 분포
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TtvDistributionResponse(
        double averageDays,
        double medianDays,
        long totalUsers,
        long usersWithEvent,
        double activationRate,
        List<TtvBucket> distribution
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TtvBucket(
            String label,
            int minDays,
            int maxDays,
            long count,
            double percentage
    ) {
        public static TtvBucket of(String label, int minDays, int maxDays, long count, double percentage) {
            return new TtvBucket(label, minDays, maxDays, count, percentage);
        }
    }

    public static TtvDistributionResponse of(
            double averageDays,
            double medianDays,
            long totalUsers,
            long usersWithEvent,
            double activationRate,
            List<TtvBucket> distribution
    ) {
        return new TtvDistributionResponse(averageDays, medianDays, totalUsers, usersWithEvent, activationRate, distribution);
    }
}
