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

        String contentType, // TEXT or HTML (기본값: TEXT)

        List<Long> userIds // 선택: to 리스트와 동일 인덱스로 매핑되는 userId 목록
) {
    public String getContentType() {
        return contentType != null ? contentType : "TEXT";
    }

    public Long getUserIdAt(int index) {
        if (userIds == null || index >= userIds.size()) {
            return null;
        }
        return userIds.get(index);
    }
}
