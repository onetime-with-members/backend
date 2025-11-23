package side.onetime.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.Status;
import side.onetime.global.common.dao.BaseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "events")
@SQLDelete(sql = "UPDATE events SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE events_id = ?")
@SQLRestriction("status = 'ACTIVE'")
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "events_id")
    private Long id;

    @Column(name = "events_uuid", columnDefinition = "BINARY(16)", unique = true)
    private UUID eventId;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "qr_file_name")
    private String qrFileName;

    @OneToMany(mappedBy = "event",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Member> members;

    @OneToMany(mappedBy = "event",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "event",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventParticipation> eventParticipations;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    @Builder
    public Event(UUID eventId, String title, String startTime, String endTime, Category category) {
        this.eventId = eventId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.status = Status.ACTIVE;
    }

    public void updateTitle(String title) {
        if (title != null) {
            this.title = title;
        }
    }

    public void updateStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void updateEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void addQrFileName(String qrFileName) {
        this.qrFileName = qrFileName;
    }
}
