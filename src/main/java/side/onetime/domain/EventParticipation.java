package side.onetime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import side.onetime.domain.enums.ParticipationRole;
import side.onetime.global.common.dao.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "event_participations")
public class EventParticipation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_participations_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "events_id", foreignKey = @ForeignKey(name = "event_participations_fk_events_id"))
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", foreignKey = @ForeignKey(name = "event_participations_fk_users_id"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_role", nullable = false)
    private ParticipationRole participationRole;

    @Builder
    public EventParticipation(Event event, User user, ParticipationRole participationRole) {
        this.event = event;
        this.user = user;
        this.participationRole = participationRole;
    }

    public void updateParticipationRole(ParticipationRole participationRole) {
        this.participationRole = participationRole;
    }
}
