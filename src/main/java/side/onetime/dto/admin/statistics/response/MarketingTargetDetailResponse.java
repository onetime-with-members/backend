package side.onetime.dto.admin.statistics.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketingTargetDetailResponse(
        String type,
        long totalCount,
        List<UserDetail> users,
        List<EventDetail> events
) {
    public static MarketingTargetDetailResponse ofUsers(String type, long totalCount, List<UserDetail> users) {
        return new MarketingTargetDetailResponse(type, totalCount, users, null);
    }

    public static MarketingTargetDetailResponse ofEvents(String type, long totalCount, List<EventDetail> events) {
        return new MarketingTargetDetailResponse(type, totalCount, null, events);
    }

    @Builder
    public record UserDetail(
            Long userId,
            String email,
            String name,
            String nickname,
            String provider,
            String providerId,
            Boolean servicePolicyAgreement,
            Boolean privacyPolicyAgreement,
            Boolean marketingPolicyAgreement,
            String sleepStartTime,
            String sleepEndTime,
            String language,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            // Optional extra fields depending on query
            LocalDateTime lastLogin,
            Integer daysInactive,
            Integer daysSinceSignup,
            Integer eventCount
    ) {
        public static UserDetail fromRow(Object[] row, String queryType) {
            UserDetailBuilder builder = UserDetail.builder()
                    .userId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .email((String) row[1])
                    .name((String) row[2])
                    .nickname((String) row[3])
                    .provider((String) row[4])
                    .providerId((String) row[5])
                    .servicePolicyAgreement(toBoolean(row[6]))
                    .privacyPolicyAgreement(toBoolean(row[7]))
                    .marketingPolicyAgreement(toBoolean(row[8]))
                    .sleepStartTime((String) row[9])
                    .sleepEndTime((String) row[10])
                    .language((String) row[11])
                    .createdDate(row[12] != null ? ((java.sql.Timestamp) row[12]).toLocalDateTime() : null)
                    .updatedDate(row[13] != null ? ((java.sql.Timestamp) row[13]).toLocalDateTime() : null);

            // Handle extra fields based on query type
            switch (queryType) {
                case "dormant" -> {
                    if (row.length > 14) {
                        builder.lastLogin(row[14] != null ? ((java.sql.Timestamp) row[14]).toLocalDateTime() : null);
                    }
                    if (row.length > 15) {
                        builder.daysInactive(row[15] != null ? ((Number) row[15]).intValue() : null);
                    }
                }
                case "noEvent" -> {
                    if (row.length > 14) {
                        builder.daysSinceSignup(row[14] != null ? ((Number) row[14]).intValue() : null);
                    }
                }
                case "oneTime", "vip" -> {
                    if (row.length > 14) {
                        builder.eventCount(row[14] != null ? ((Number) row[14]).intValue() : null);
                    }
                }
                case "agreed" -> {
                    // No extra fields
                }
            }

            return builder.build();
        }

        private static Boolean toBoolean(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof Number) return ((Number) value).intValue() == 1;
            return false;
        }
    }

    @Builder
    public record EventDetail(
            Long eventId,
            String title,
            String category,
            String startTime,
            String endTime,
            LocalDateTime createdDate,
            Long creatorId,
            String creatorEmail,
            String creatorName,
            String creatorNickname,
            Integer daysSinceCreated
    ) {
        public static EventDetail fromRow(Object[] row) {
            return EventDetail.builder()
                    .eventId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .title((String) row[1])
                    .category((String) row[2])
                    .startTime((String) row[3])
                    .endTime((String) row[4])
                    .createdDate(row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null)
                    .creatorId(row[6] != null ? ((Number) row[6]).longValue() : null)
                    .creatorEmail((String) row[7])
                    .creatorName((String) row[8])
                    .creatorNickname((String) row[9])
                    .daysSinceCreated(row[10] != null ? ((Number) row[10]).intValue() : null)
                    .build();
        }
    }
}
