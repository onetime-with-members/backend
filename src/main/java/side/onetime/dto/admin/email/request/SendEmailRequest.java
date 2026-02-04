package side.onetime.dto.admin.email.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SendEmailRequest(
        @NotEmpty(message = "수신자 이메일은 필수입니다.")
        List<String> to,

        @NotBlank(message = "제목은 필수입니다.")
        String subject,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        String contentType,

        @NotEmpty(message = "수신자 userId는 필수입니다.")
        List<Long> userIds
) {
    public SendEmailRequest {
        if (to != null && userIds != null && to.size() != userIds.size()) {
            throw new IllegalArgumentException("수신자 이메일과 userId 개수가 일치해야 합니다.");
        }
    }

    public String getContentType() {
        return contentType != null ? contentType : "TEXT";
    }

    public Long getUserIdAt(int index) {
        return userIds.get(index);
    }
}
