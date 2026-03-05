package side.onetime.dto.user.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Size;
import side.onetime.domain.enums.Language;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateUserProfileRequest(
        @Size(max = 50, message = "닉네임은 최대 50자까지 가능합니다.")
        String nickname,
        Language language
) {
}
