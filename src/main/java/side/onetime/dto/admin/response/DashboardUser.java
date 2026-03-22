package side.onetime.dto.admin.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.User;
import side.onetime.domain.enums.Language;

import java.time.format.DateTimeFormatter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardUser(
        Long id,
        String name,
        String email,
        String nickname,
        String provider,
        String providerId,
        Boolean servicePolicyAgreement,
        Boolean privacyPolicyAgreement,
        Boolean marketingPolicyAgreement,
        String sleepStartTime,
        String sleepEndTime,
        Language language,
        int participantCount,
        String createdDate
) {
    public static DashboardUser from(User user, int participantCount) {
        return new DashboardUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                user.getServicePolicyAgreement(),
                user.getPrivacyPolicyAgreement(),
                user.getMarketingPolicyAgreement(),
                user.getSleepStartTime(),
                user.getSleepEndTime(),
                user.getLanguage(),
                participantCount,
                user.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}
