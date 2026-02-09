package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.NotBlank;

public record SendTestEmailRequest(
        @NotBlank(message = "제목은 필수입니다.")
        String subject,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        String contentType
) {
    public String getContentType() {
        return contentType != null ? contentType : "TEXT";
    }
}
