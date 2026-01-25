package side.onetime.dto.banner.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.BarBanner;

import java.time.format.DateTimeFormatter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetBarBannerResponse(
        Long id,
        String contentKor,
        String contentEng,
        String backgroundColorCode,
        String textColorCode,
        Boolean isActivated,
        String createdDate,
        String linkUrl
) {
    public static GetBarBannerResponse from(BarBanner barBanner) {
        return new GetBarBannerResponse(
                barBanner.getId(),
                barBanner.getContentKor(),
                barBanner.getContentEng(),
                barBanner.getBackgroundColorCode(),
                barBanner.getTextColorCode(),
                barBanner.getIsActivated(),
                barBanner.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                barBanner.getLinkUrl()
        );
    }
}
