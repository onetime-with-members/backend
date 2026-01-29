package side.onetime.dto.admin.statistics.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserStatisticsResponse(
        long totalUsers,
        long activeUsers,
        long deletedUsers,
        double marketingAgreementRate,
        Map<String, Long> providerDistribution,
        Map<String, Long> languageDistribution,
        List<MonthlyData> monthlySignups
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyData(
            String month,
            long count
    ) {}

    public static UserStatisticsResponse of(
            long totalUsers,
            long activeUsers,
            long deletedUsers,
            double marketingAgreementRate,
            Map<String, Long> providerDistribution,
            Map<String, Long> languageDistribution,
            List<MonthlyData> monthlySignups
    ) {
        return new UserStatisticsResponse(
                totalUsers, activeUsers, deletedUsers,
                marketingAgreementRate, providerDistribution,
                languageDistribution, monthlySignups
        );
    }
}
