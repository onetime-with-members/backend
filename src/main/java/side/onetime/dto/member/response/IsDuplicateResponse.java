package side.onetime.dto.member.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record IsDuplicateResponse(
        Boolean isPossible
) {
    public static IsDuplicateResponse of(Boolean isPossible) {
        return new IsDuplicateResponse(isPossible);
    }
}
