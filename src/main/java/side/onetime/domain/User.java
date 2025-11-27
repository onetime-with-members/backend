package side.onetime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import side.onetime.domain.enums.Language;
import side.onetime.domain.enums.Status;
import side.onetime.global.common.dao.BaseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE users_id = ?")
@SQLRestriction("status = 'ACTIVE'")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_id", length = 50, unique = true)
    private String providerId;

    @Column(name = "service_policy_agreement")
    private Boolean servicePolicyAgreement;

    @Column(name = "privacy_policy_agreement")
    private Boolean privacyPolicyAgreement;

    @Column(name = "marketing_policy_agreement")
    private Boolean marketingPolicyAgreement;

    @Column(name = "sleep_start_time", length = 10)
    private String sleepStartTime;

    @Column(name = "sleep_end_time", length = 10)
    private String sleepEndTime;

    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    private Language language;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Selection> selections;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventParticipation> eventParticipations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FixedSelection> fixedSelections;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    @Builder
    public User(String name, String email, String nickname, String provider, String providerId, Boolean servicePolicyAgreement, Boolean privacyPolicyAgreement, Boolean marketingPolicyAgreement, String sleepStartTime, String sleepEndTime, Language language) {
        this.name = name;
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.servicePolicyAgreement = servicePolicyAgreement;
        this.privacyPolicyAgreement = privacyPolicyAgreement;
        this.marketingPolicyAgreement = marketingPolicyAgreement;
        this.sleepStartTime = sleepStartTime;
        this.sleepEndTime = sleepEndTime;
        this.language = language;
        this.status = Status.ACTIVE;
    }

    public void updateNickName(String nickname) {
        this.nickname = nickname;
    }

    public void updateServicePolicyAgreement(Boolean servicePolicyAgreement) {
        this.servicePolicyAgreement = servicePolicyAgreement;
    }

    public void updatePrivacyPolicyAgreement(Boolean privacyPolicyAgreement) {
        this.privacyPolicyAgreement = privacyPolicyAgreement;
    }

    public void updateMarketingPolicyAgreement(Boolean marketingPolicyAgreement) {
        this.marketingPolicyAgreement = marketingPolicyAgreement;
    }

    public void updateSleepStartTime(String sleepStartTime) {
        this.sleepStartTime = sleepStartTime;
    }

    public void updateSleepEndTime(String sleepEndTime) {
        this.sleepEndTime = sleepEndTime;
    }

    public void updateLanguage(Language language) {
        this.language = language;
    }
}
