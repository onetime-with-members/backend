package side.onetime.dto.url.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConvertToShortenUrlRequest(
        @NotBlank(message = "Original URL은 필수 값입니다.") String originalUrl
) {
}
