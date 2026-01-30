package side.onetime.domain;

import java.time.LocalDateTime;

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
import side.onetime.domain.enums.EmailLogStatus;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "content_type", length = 10)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailLogStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "target_group", length = 50)
    private String targetGroup;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Builder
    public EmailLog(String recipient, String subject, String contentType,
                    EmailLogStatus status, String errorMessage, String targetGroup) {
        this.recipient = recipient;
        this.subject = subject;
        this.contentType = contentType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.targetGroup = targetGroup;
        this.sentAt = LocalDateTime.now();
    }

    public void updateStatus(EmailLogStatus status) {
        this.status = status;
    }

    public void updateStatus(EmailLogStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
