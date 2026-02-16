package side.onetime.dto.user.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetGuideViewLogResponse(
        boolean isViewed
) {
    public static GetGuideViewLogResponse from(boolean isViewed) {
        return new GetGuideViewLogResponse(isViewed);
    }
}
