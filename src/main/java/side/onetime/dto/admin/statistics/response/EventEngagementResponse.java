package side.onetime.dto.admin.statistics.response;

import java.util.List;

/**
 * 이벤트 참여 통계 응답 DTO
 * - 이벤트 완료율, 참여자 응답률, 평균 응답 시간
 * - 멤버 수 분포, 익명 vs 회원 비율, 이벤트 삭제율
 */
public record EventEngagementResponse(
        // 이벤트 완료율 (1명 이상 응답받은 이벤트 비율)
        long totalEvents,
        long eventsWithResponse,
        double completionRate,

        // 참여자 응답률 (Selection 생성한 Member 비율)
        long totalMembers,
        long membersWithSelection,
        double responseRate,

        // 평균 응답 시간 (이벤트 생성 → 첫 Selection까지 시간, 단위: 시간)
        double avgResponseTimeHours,
        double medianResponseTimeHours,

        // 멤버 수 분포
        List<MemberCountBucket> memberCountDistribution,

        // 익명 vs 회원 비율
        long anonymousParticipants,  // Member 기반
        long registeredParticipants, // User 기반 (EventParticipation PARTICIPANT)
        double anonymousRate,

        // 이벤트 삭제율
        long deletedEvents,
        double deletionRate
) {
    public static EventEngagementResponse of(
            long totalEvents, long eventsWithResponse, double completionRate,
            long totalMembers, long membersWithSelection, double responseRate,
            double avgResponseTimeHours, double medianResponseTimeHours,
            List<MemberCountBucket> memberCountDistribution,
            long anonymousParticipants, long registeredParticipants, double anonymousRate,
            long deletedEvents, double deletionRate) {
        return new EventEngagementResponse(
                totalEvents, eventsWithResponse, completionRate,
                totalMembers, membersWithSelection, responseRate,
                avgResponseTimeHours, medianResponseTimeHours,
                memberCountDistribution,
                anonymousParticipants, registeredParticipants, anonymousRate,
                deletedEvents, deletionRate
        );
    }

    /**
     * 멤버 수 분포 버킷
     */
    public record MemberCountBucket(
            String label,      // "0명", "1명", "2-3명", "4-5명", "6-10명", "11명+"
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
