package side.onetime.dto.test.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TestTokenResponse(
        String accessToken,
        String refreshToken
) {
    public static TestTokenResponse of(String accessToken, String refreshToken) {
        return new TestTokenResponse(accessToken, refreshToken);
    }

    public static TestTokenResponse ofAccessToken(String accessToken) {
        return new TestTokenResponse(accessToken, null);
    }
}
