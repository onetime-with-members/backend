package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailTemplateRequest(
        @NotBlank(message = "템플릿 이름은 필수입니다")
        @Size(max = 100, message = "템플릿 이름은 100자 이내여야 합니다")
        String name,

        @Size(max = 50, message = "템플릿 코드는 50자 이내여야 합니다")
        String code,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다")
        String subject,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        String contentType
) {
}
