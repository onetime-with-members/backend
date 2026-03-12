package side.onetime.dto.user.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateUserPolicyAgreementRequest(
        @NotNull(message = "서비스 이용약관 동의여부는 필수 값입니다.")
        Boolean servicePolicyAgreement,
        @NotNull(message = "개인정보 수집 및 이용 동의여부는 필수 값입니다.")
        Boolean privacyPolicyAgreement,
        @NotNull(message = "마케팅 정보 수신 동의여부는 필수 값입니다.")
        Boolean marketingPolicyAgreement
) {
}
