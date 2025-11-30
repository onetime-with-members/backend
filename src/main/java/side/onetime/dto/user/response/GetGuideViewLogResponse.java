package side.onetime.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetGuideViewLogResponse(
        boolean isViewed
) {
    public static GetGuideViewLogResponse from(boolean isViewed) {
        return new GetGuideViewLogResponse(isViewed);
    }
}
