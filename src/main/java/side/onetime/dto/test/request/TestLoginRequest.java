package side.onetime.dto.test.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TestLoginRequest(
        @NotBlank(message = "시크릿 키는 필수입니다.")
        String secretKey
) {
}
