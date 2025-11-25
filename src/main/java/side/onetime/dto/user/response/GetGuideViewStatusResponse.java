package side.onetime.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetGuideViewStatusResponse(
        boolean isViewed
) {
    public static GetGuideViewStatusResponse from(boolean isViewed) {
        return new GetGuideViewStatusResponse(isViewed);
    }
}
