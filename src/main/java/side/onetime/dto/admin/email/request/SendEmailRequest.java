package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SendEmailRequest(
        @NotEmpty(message = "수신자 이메일은 필수입니다.")
        List<String> to,

        @NotBlank(message = "제목은 필수입니다.")
        String subject,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        String contentType // TEXT or HTML (기본값: TEXT)
) {
    public String getContentType() {
        return contentType != null ? contentType : "TEXT";
    }
}
