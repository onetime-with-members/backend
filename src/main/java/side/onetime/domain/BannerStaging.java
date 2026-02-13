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
@Table(name = "banner_staging")
public class BannerStaging extends BaseEntity {

    @Id
    @Column(name = "banners_id")
    private Long bannerId;

    @Column(name = "organization", nullable = false, length = 200)
    private String organization;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "sub_title", nullable = false, length = 200)
    private String subTitle;

    @Column(name = "button_text", nullable = false, length = 200)
    private String buttonText;

    @Column(name = "color_code", nullable = false, length = 30)
    private String colorCode;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "link_url", length = 200)
    private String linkUrl;

    @Builder
    public BannerStaging(Long bannerId, String organization, String title, String subTitle, String buttonText, String colorCode, String imageUrl, String linkUrl) {
        this.bannerId = bannerId;
        this.organization = organization;
        this.title = title;
        this.subTitle = subTitle;
        this.buttonText = buttonText;
        this.colorCode = colorCode;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }
}
