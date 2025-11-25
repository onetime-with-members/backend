package side.onetime.dto.user.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import side.onetime.domain.enums.GuideType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateGuideViewStatusRequest(
        @NotNull(message = "가이드 타입은 필수 값입니다.")
        GuideType guideType
) {
}
