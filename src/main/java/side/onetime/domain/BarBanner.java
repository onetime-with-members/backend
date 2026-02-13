package side.onetime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.global.common.dao.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "bar_banners")
public class BarBanner extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bar_banners_id")
    private Long id;

    @Column(name = "bar_banner_staging_id", unique = true)
    private Long barBannerStagingId;

    @Column(name = "content_kor", nullable = false, length = 200)
    private String contentKor;

    @Column(name = "content_eng", nullable = false, length = 200)
    private String contentEng;

    @Column(name = "background_color_code", nullable = false, length = 30)
    private String backgroundColorCode;

    @Column(name = "text_color_code", nullable = false, length = 30)
    private String textColorCode;

    @Column(name = "is_activated", nullable = false)
    private Boolean isActivated;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "link_url", length = 200)
    private String linkUrl;

    @Builder
    public BarBanner(Long barBannerStagingId, String contentKor, String contentEng, String backgroundColorCode, String textColorCode, String linkUrl) {
        this.barBannerStagingId = barBannerStagingId;
        this.contentKor = contentKor;
        this.contentEng = contentEng;
        this.backgroundColorCode = backgroundColorCode;
        this.textColorCode = textColorCode;
        this.isActivated = false;
        this.isDeleted = false;
        this.linkUrl = linkUrl;
    }

    public void updateContentKor(String contentKor) {
        this.contentKor = contentKor;
    }

    public void updateContentEng(String contentEng) {
        this.contentEng = contentEng;
    }

    public void updateBackgroundColorCode(String backgroundColorCode) {
        this.backgroundColorCode = backgroundColorCode;
    }

    public void updateTextColorCode(String textColorCode) {
        this.textColorCode = textColorCode;
    }

    public void updateIsActivated(Boolean isActivated) {
        this.isActivated = isActivated;
    }

    public void updateLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.isActivated = false;
    }
}
