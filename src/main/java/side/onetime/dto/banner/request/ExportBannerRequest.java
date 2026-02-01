package side.onetime.dto.banner.request;

import side.onetime.domain.Banner;
import side.onetime.domain.BannerStaging;

public record ExportBannerRequest(
        Long bannerId,
        String organization,
        String title,
        String subTitle,
        String buttonText,
        String colorCode,
        String imageUrl,
        String linkUrl
) {
    public static ExportBannerRequest from(Banner banner) {
        return new ExportBannerRequest(
                banner.getId(),
                banner.getOrganization(),
                banner.getTitle(),
                banner.getSubTitle(),
                banner.getButtonText(),
                banner.getColorCode(),
                banner.getImageUrl(),
                banner.getLinkUrl()
        );
    }

    public BannerStaging toEntity() {
        return BannerStaging.builder()
                .bannerId(this.bannerId)
                .organization(this.organization)
                .title(this.title)
                .subTitle(this.subTitle)
                .buttonText(this.buttonText)
                .colorCode(this.colorCode)
                .imageUrl(this.imageUrl)
                .linkUrl(this.linkUrl)
                .build();
    }
}
