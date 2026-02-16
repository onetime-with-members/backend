package side.onetime.dto.user.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import side.onetime.domain.enums.Language;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OnboardUserRequest(
        @NotBlank(message = "레지스터 토큰은 필수 값입니다.")
        String registerToken,
        @NotBlank(message = "닉네임은 필수 값입니다.")
        @Size(max = 50, message = "닉네임은 최대 50자까지 가능합니다.")
        String nickname,
        @NotNull(message = "서비스 이용약관 동의여부는 필수 값입니다.")
        Boolean servicePolicyAgreement,
        @NotNull(message = "개인정보 수집 및 이용 동의여부는 필수 값입니다.")
        Boolean privacyPolicyAgreement,
        @NotNull(message = "마케팅 정보 수신 동의여부는 필수 값입니다.")
        Boolean marketingPolicyAgreement,
        @NotBlank(message = "수면 시작 시간은 필수 값입니다.")
        String sleepStartTime,
        @NotBlank(message = "수면 종료 시간은 필수 값입니다.")
        String sleepEndTime,
        @NotNull(message = "언어는 필수 값입니다.")
        Language language
) {
}
