package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import side.onetime.domain.EmailTemplate;

public record CreateEmailTemplateRequest(
        @NotBlank(message = "템플릿 이름은 필수입니다")
        @Size(max = 100, message = "템플릿 이름은 100자 이내여야 합니다")
        String name,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다")
        String subject,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        String contentType
) {
    public EmailTemplate toEntity() {
        return EmailTemplate.builder()
                .name(name)
                .subject(subject)
                .content(content)
                .contentType(contentType != null ? contentType : "TEXT")
                .build();
    }
}
