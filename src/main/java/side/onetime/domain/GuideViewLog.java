package side.onetime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.domain.enums.GuideType;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "guide_view_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_guide_type", columnNames = {"users_id", "guide_type"})
        }
)
public class GuideViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_view_logs_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", foreignKey = @ForeignKey(name = "guide_view_logs_fk_users_id"), nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "guide_type", nullable = false)
    private GuideType guideType;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Builder
    public GuideViewLog(User user, GuideType guideType) {
        this.user = user;
        this.guideType = guideType;
        this.viewedAt = LocalDateTime.now();
    }
}
