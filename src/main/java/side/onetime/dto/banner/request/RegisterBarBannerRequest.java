package side.onetime.dto.banner.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import side.onetime.domain.BarBanner;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterBarBannerRequest(

        @NotBlank(message = "한글 내용은 비어 있을 수 없습니다.")
        @Size(max = 200, message = "한글 내용은 최대 200자까지 가능합니다.")
        String contentKor,

        @NotBlank(message = "영문 내용은 비어 있을 수 없습니다.")
        @Size(max = 200, message = "영문 내용은 최대 200자까지 가능합니다.")
        String contentEng,

        @NotBlank(message = "배경 색상 값은 비어 있을 수 없습니다.")
        @Size(max = 30, message = "배경 색상 값은 최대 30자까지 가능합니다.")
        String backgroundColorCode,

        @NotBlank(message = "텍스트 색상 값은 비어 있을 수 없습니다.")
        @Size(max = 30, message = "텍스트 색상 값은 최대 30자까지 가능합니다.")
        String textColorCode,

        @Size(max = 200, message = "링크 URL은 최대 200자까지 가능합니다.")
        String linkUrl
) {

        public BarBanner toEntity() {
                return BarBanner.builder()
                        .contentKor(contentKor)
                        .contentEng(contentEng)
                        .backgroundColorCode(backgroundColorCode)
                        .textColorCode(textColorCode)
                        .linkUrl(linkUrl)
                        .build();
        }
}
