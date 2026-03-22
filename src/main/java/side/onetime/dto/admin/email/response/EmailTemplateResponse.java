package side.onetime.dto.admin.email.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import side.onetime.domain.EmailTemplate;

public record EmailTemplateResponse(
        Long id,
        String name,
        String code,
        String subject,
        String content,
        String contentType,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static EmailTemplateResponse from(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getName(),
                template.getCode(),
                template.getSubject(),
                template.getContent(),
                template.getContentType(),
                template.getCreatedDate(),
                template.getUpdatedDate()
        );
    }
}
