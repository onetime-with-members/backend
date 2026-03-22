package side.onetime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.domain.enums.AdminStatus;
import side.onetime.global.common.dao.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "admin_users")
public class AdminUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_users_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false)
    private AdminStatus adminStatus;

    @Builder
    public AdminUser(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.adminStatus = AdminStatus.PENDING_APPROVAL;
    }

    public void updateAdminStatus(AdminStatus adminStatus) {
        this.adminStatus = adminStatus;
    }
}
