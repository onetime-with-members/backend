package side.onetime.dto.admin.email.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record SendToGroupRequest(
        @NotBlank(message = "대상 그룹은 필수입니다.")
        String targetGroup, // agreed, dormant, noEvent, oneTime, vip

        @NotBlank(message = "제목은 필수입니다.")
        String subject,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        String contentType, // TEXT or HTML (기본값: TEXT)

        @Max(value = 1000, message = "최대 1000건까지만 발송 가능합니다.")
        Integer limit // 발송 대상 수 제한 (기본값: 100)
) {
    public String getContentType() {
        return contentType != null ? contentType : "TEXT";
    }

    public int getLimit() {
        return limit != null ? limit : 100;
    }
}
