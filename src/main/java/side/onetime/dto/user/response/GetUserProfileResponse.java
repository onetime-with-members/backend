package side.onetime.dto.user.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.User;
import side.onetime.domain.enums.Language;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetUserProfileResponse(
        String nickname,
        String email,
        Language language,
        String socialPlatform
) {
    public static GetUserProfileResponse of(User user) {
        return new GetUserProfileResponse(
                user.getNickname(),
                user.getEmail(),
                user.getLanguage(),
                user.getProvider()
        );
    }
}
