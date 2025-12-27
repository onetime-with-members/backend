package side.onetime.dto.banner.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import side.onetime.domain.Banner;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterBannerRequest(

        @NotBlank(message = "조직명은 비어 있을 수 없습니다.")
        @Size(max = 200, message = "조직명은 최대 200자까지 가능합니다.")
        String organization,

        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        @Size(max = 200, message = "제목은 최대 200자까지 가능합니다.")
        String title,

        @NotBlank(message = "부제목은 비어 있을 수 없습니다.")
        @Size(max = 200, message = "부제목은 최대 200자까지 가능합니다.")
        String subTitle,

        @NotBlank(message = "버튼 텍스트는 비어 있을 수 없습니다.")
        @Size(max = 200, message = "버튼 텍스트는 최대 200자까지 가능합니다.")
        String buttonText,

        @NotBlank(message = "색상 값은 비어 있을 수 없습니다.")
        @Size(max = 30, message = "색상 값은 최대 30자까지 가능합니다.")
        String colorCode,

        @Size(max = 200, message = "링크 URL은 최대 200자까지 가능합니다.")
        String linkUrl
) {

    public Banner toEntity() {
        return Banner.builder()
                .organization(organization)
                .title(title)
                .subTitle(subTitle)
                .buttonText(buttonText)
                .colorCode(colorCode)
                .linkUrl(linkUrl)
                .build();
    }
}
