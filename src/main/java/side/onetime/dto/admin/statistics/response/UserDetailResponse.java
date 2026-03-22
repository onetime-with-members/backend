package side.onetime.dto.admin.statistics.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserDetailResponse(
        Long userId,
        String name,
        String nickname,
        String email,
        String provider,
        String language,
        String createdDate,
        boolean marketingAgreement,
        String lastUsedAt,
        String lastUsedIp,
        int activeSessionCount,
        int inactiveDays,
        int createdEventCount,
        int participatedEventCount
) {
    public static UserDetailResponse of(
            Long userId,
            String name,
            String nickname,
            String email,
            String provider,
            String language,
            String createdDate,
            boolean marketingAgreement,
            String lastUsedAt,
            String lastUsedIp,
            int activeSessionCount,
            int inactiveDays,
            int createdEventCount,
            int participatedEventCount
    ) {
        return new UserDetailResponse(
                userId, name, nickname, email, provider, language, createdDate,
                marketingAgreement, lastUsedAt, lastUsedIp, activeSessionCount,
                inactiveDays, createdEventCount, participatedEventCount
        );
    }
}
