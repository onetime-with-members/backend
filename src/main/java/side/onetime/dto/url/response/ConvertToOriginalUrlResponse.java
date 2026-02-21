package side.onetime.dto.url.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConvertToOriginalUrlResponse(
        String originalUrl
) {
    public static ConvertToOriginalUrlResponse of(String originalUrl) {
        return new ConvertToOriginalUrlResponse(originalUrl);
    }
}
