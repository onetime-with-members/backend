package side.onetime.dto.banner.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.Banner;

import java.time.format.DateTimeFormatter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetBannerResponse(
        Long id,
        String organization,
        String title,
        String subTitle,
        String buttonText,
        String colorCode,
        String imageUrl,
        Boolean isActivated,
        String createdDate,
        String linkUrl,
        Long clickCount
) {
    public static GetBannerResponse from(Banner banner) {
        return new GetBannerResponse(
                banner.getId(),
                banner.getOrganization(),
                banner.getTitle(),
                banner.getSubTitle(),
                banner.getButtonText(),
                banner.getColorCode(),
                banner.getImageUrl(),
                banner.getIsActivated(),
                banner.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                banner.getLinkUrl(),
                banner.getClickCount()
        );
    }
}
