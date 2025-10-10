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
@Table(name = "banners")
public class Banner extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banners_id")
    private Long id;

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

    @Column(name = "is_activated", nullable = false)
    private Boolean isActivated;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "link_url", length = 200)
    private String linkUrl;

    @Column(name = "click_count", nullable = false, columnDefinition = "INT UNSIGNED")
    private Long clickCount;

    @Builder
    public Banner(String organization, String title, String subTitle, String buttonText, String colorCode, String imageUrl, String linkUrl) {
        this.organization = organization;
        this.title = title;
        this.subTitle = subTitle;
        this.buttonText = buttonText;
        this.colorCode = colorCode;
        this.imageUrl = imageUrl;
        this.isActivated = false;
        this.isDeleted = false;
        this.linkUrl = linkUrl;
        this.clickCount = 0L;
    }

    public void updateOrganization(String organization) {
        this.organization = organization;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public void updateButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public void updateColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
