package side.onetime.dto.user.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.User;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetUserPolicyAgreementResponse(
        Boolean servicePolicyAgreement,
        Boolean privacyPolicyAgreement,
        Boolean marketingPolicyAgreement
) {
    public static GetUserPolicyAgreementResponse from(User user) {
        return new GetUserPolicyAgreementResponse(
                user.getServicePolicyAgreement() != null ? user.getServicePolicyAgreement() : false,
                user.getPrivacyPolicyAgreement() != null ? user.getPrivacyPolicyAgreement() : false,
                user.getMarketingPolicyAgreement() != null ? user.getMarketingPolicyAgreement() : false
        );
    }
}
