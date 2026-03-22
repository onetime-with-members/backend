package side.onetime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "email_templates")
public class EmailTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", nullable = false, length = 10)
    private String contentType;

    @Builder
    public EmailTemplate(String name, String code, String subject, String content, String contentType) {
        this.name = name;
        this.code = code;
        this.subject = subject;
        this.content = content;
        this.contentType = contentType != null ? contentType : "TEXT";
    }

    public void update(String name, String code, String subject, String content, String contentType) {
        this.name = name;
        this.code = code;
        this.subject = subject;
        this.content = content;
        this.contentType = contentType;
    }
}
