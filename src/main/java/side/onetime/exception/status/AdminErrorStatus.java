package side.onetime.exception.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import side.onetime.global.common.code.BaseErrorCode;
import side.onetime.global.common.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum AdminErrorStatus implements BaseErrorCode {
    // AdminUser
    _IS_DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "ADMIN-USER-001", "이미 존재하는 이메일입니다."),
    _NOT_FOUND_ADMIN_USER(HttpStatus.NOT_FOUND, "ADMIN-USER-002", "관리자 계정을 찾을 수 없습니다."),
    _IS_NOT_APPROVED_ADMIN_USER(HttpStatus.UNAUTHORIZED, "ADMIN-USER-003", "승인되지 않은 관리자 계정입니다."),
    _IS_NOT_EQUAL_PASSWORD(HttpStatus.BAD_REQUEST, "ADMIN-USER-004", "등록된 비밀번호와 다릅니다."),
    _ONLY_CAN_MASTER_ADMIN_USER(HttpStatus.UNAUTHORIZED, "ADMIN-USER-005", "마스터 관리자만 사용 가능한 기능입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ADMIN-USER-011", "인증된 관리자가 아닙니다."),
    // DashBoard
    _INVALID_SORT_KEYWORD(HttpStatus.BAD_REQUEST, "ADMIN-USER-006", "지원하지 않는 정렬 기준입니다."),
    // Banner
    _NOT_FOUND_BAR_BANNER(HttpStatus.NOT_FOUND, "ADMIN-USER-007", "띠배너를 찾을 수 없습니다."),
    _NOT_FOUND_ACTIVATED_BAR_BANNER(HttpStatus.NOT_FOUND, "ADMIN-USER-008", "활성화된 띠배너를 찾을 수 없습니다."),
    _NOT_FOUND_BANNER(HttpStatus.NOT_FOUND, "ADMIN-USER-009", "배너를 찾을 수 없습니다."),
    _FAILED_UPLOAD_BANNER_IMAGE(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN-USER-010", "배너 이미지 업로드 하는 과정에서 문제가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
