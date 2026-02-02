package side.onetime.dto.banner.request;

import side.onetime.domain.BarBanner;
import side.onetime.domain.BarBannerStaging;

public record ExportBarBannerRequest(
        Long barBannerId,
        String contentKor,
        String contentEng,
        String backgroundColorCode,
        String textColorCode,
        String linkUrl
) {
    public static ExportBarBannerRequest from(BarBanner barBanner) {
        return new ExportBarBannerRequest(
                barBanner.getId(),
                barBanner.getContentKor(),
                barBanner.getContentEng(),
                barBanner.getBackgroundColorCode(),
                barBanner.getTextColorCode(),
                barBanner.getLinkUrl()
        );
    }

    public BarBannerStaging toEntity() {
        return BarBannerStaging.builder()
                .barBannerId(this.barBannerId)
                .contentKor(this.contentKor)
                .contentEng(this.contentEng)
                .backgroundColorCode(this.backgroundColorCode)
                .textColorCode(this.textColorCode)
                .linkUrl(this.linkUrl)
                .build();
    }
}
