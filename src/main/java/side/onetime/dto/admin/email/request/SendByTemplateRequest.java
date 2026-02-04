package side.onetime.dto.admin.email.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SendByTemplateRequest(
        @NotBlank(message = "템플릿 코드는 필수입니다")
        String templateCode,

        @NotEmpty(message = "수신자 이메일은 필수입니다")
        List<String> to,

        @NotEmpty(message = "수신자 userId는 필수입니다")
        List<Long> userIds
) {
}
