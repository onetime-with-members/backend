package side.onetime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.global.common.dao.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "bar_banner_staging")
public class BarBannerStaging extends BaseEntity {

    @Id
    @Column(name = "bar_banners_id")
    private Long barBannerId;

    @Column(name = "content_kor", nullable = false, length = 200)
    private String contentKor;

    @Column(name = "content_eng", nullable = false, length = 200)
    private String contentEng;

    @Column(name = "background_color_code", nullable = false, length = 30)
    private String backgroundColorCode;

    @Column(name = "text_color_code", nullable = false, length = 30)
    private String textColorCode;

    @Column(name = "link_url", length = 200)
    private String linkUrl;

    @Builder
    public BarBannerStaging(Long barBannerId, String contentKor, String contentEng, String backgroundColorCode, String textColorCode, String linkUrl) {
        this.barBannerId = barBannerId;
        this.contentKor = contentKor;
        this.contentEng = contentEng;
        this.backgroundColorCode = backgroundColorCode;
        this.textColorCode = textColorCode;
        this.linkUrl = linkUrl;
    }
}
