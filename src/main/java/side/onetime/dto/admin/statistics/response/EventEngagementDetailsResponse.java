package side.onetime.dto.admin.statistics.response;

import java.util.List;

/**
 * 이벤트 참여 상세 통계 응답 DTO (느린 쿼리, 캐싱 대상)
 * - 평균 응답 시간, 멤버 수 분포
 */
public record EventEngagementDetailsResponse(
        double avgResponseTimeHours,
        double medianResponseTimeHours,
        List<MemberCountBucket> memberCountDistribution
) {
    public static EventEngagementDetailsResponse of(
            double avgResponseTimeHours,
            double medianResponseTimeHours,
            List<MemberCountBucket> memberCountDistribution) {
        return new EventEngagementDetailsResponse(
                avgResponseTimeHours, medianResponseTimeHours, memberCountDistribution
        );
    }

    public record MemberCountBucket(
            String label,
            int minCount,
            int maxCount,
            long eventCount,
            double percentage
    ) {
        public static MemberCountBucket of(String label, int min, int max, long count, double pct) {
            return new MemberCountBucket(label, min, max, count, pct);
        }
    }
}
