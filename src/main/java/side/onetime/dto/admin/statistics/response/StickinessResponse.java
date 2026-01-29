package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * WAU/MAU Stickiness Response
 * 서비스 점착도(Stickiness) = WAU / MAU × 100
 * 높을수록 유저가 자주 방문함을 의미
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StickinessResponse(
        double currentStickiness,     // 이번 달 점착도
        long currentWau,              // 이번 주 WAU
        long currentMau,              // 이번 달 MAU
        List<MonthlyStickiness> trend // 월별 트렌드
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MonthlyStickiness(
            String month,       // "2024-01"
            long wau,           // 해당 월 평균 WAU
            long mau,           // 해당 월 MAU
            double stickiness   // WAU/MAU × 100
    ) {
        public static MonthlyStickiness of(String month, long wau, long mau, double stickiness) {
            return new MonthlyStickiness(month, wau, mau, stickiness);
        }
    }

    public static StickinessResponse of(
            double currentStickiness,
            long currentWau,
            long currentMau,
            List<MonthlyStickiness> trend
    ) {
        return new StickinessResponse(currentStickiness, currentWau, currentMau, trend);
    }
}
