package side.onetime.dto.token.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReissueTokenResponse(
        String accessToken,
        String refreshToken
) {
    public static ReissueTokenResponse of(String accessToken, String refreshToken) {
        return new ReissueTokenResponse(accessToken, refreshToken);
    }
}
