package side.onetime.dto.admin.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginAdminUserResponse(
        String accessToken,
        String refreshToken
) {
    public static LoginAdminUserResponse of(String accessToken, String refreshToken) {
        return new LoginAdminUserResponse(accessToken, refreshToken);
    }
}
