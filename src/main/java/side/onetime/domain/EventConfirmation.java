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
import side.onetime.domain.enums.ParticipationRole;
import side.onetime.domain.enums.SelectionSource;
import side.onetime.global.common.dao.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "event_confirmations")
public class EventConfirmation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "events_id", nullable = false)
    private Long eventId;

    @Column(name = "users_id")
    private Long userId;

    @Column(name = "start_date", length = 10)
    private String startDate;

    @Column(name = "end_date", length = 10)
    private String endDate;

    @Column(name = "start_day", length = 10)
    private String startDay;

    @Column(name = "end_day", length = 10)
    private String endDay;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmer_role", nullable = false, length = 30)
    private ParticipationRole confirmerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 20)
    private SelectionSource selectionSource;

    @Builder
    public EventConfirmation(Long eventId, Long userId, String startDate, String endDate,
                             String startDay, String endDay, String startTime, String endTime,
                             ParticipationRole confirmerRole, SelectionSource selectionSource) {
        this.eventId = eventId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startDay = startDay;
        this.endDay = endDay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.confirmerRole = confirmerRole;
        this.selectionSource = selectionSource;
    }

    public void update(Long userId, String startDate, String endDate, String startDay, String endDay,
                       String startTime, String endTime, ParticipationRole confirmerRole,
                       SelectionSource selectionSource) {
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startDay = startDay;
        this.endDay = endDay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.confirmerRole = confirmerRole;
        this.selectionSource = selectionSource;
    }
}
