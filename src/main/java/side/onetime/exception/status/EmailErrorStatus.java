package side.onetime.exception.status;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import side.onetime.global.common.code.BaseErrorCode;
import side.onetime.global.common.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum EmailErrorStatus implements BaseErrorCode {
    _EMAIL_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL-001", "이메일 템플릿을 찾을 수 없습니다."),
    _EMAIL_TEMPLATE_NAME_DUPLICATED(HttpStatus.BAD_REQUEST, "EMAIL-002", "이미 존재하는 템플릿 이름입니다."),
    _EMAIL_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL-003", "이메일 예약을 찾을 수 없습니다."),
    _EMAIL_SCHEDULE_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "EMAIL-004", "취소할 수 없는 예약 상태입니다."),
    _EMAIL_SCHEDULE_INVALID_TIME(HttpStatus.BAD_REQUEST, "EMAIL-005", "예약 시간은 현재 시간 이후여야 합니다."),
    _EMAIL_SCHEDULE_INVALID_TIME_FORMAT(HttpStatus.BAD_REQUEST, "EMAIL-006", "예약 시간 형식이 올바르지 않습니다. (예: 2025-03-01T10:00:00)"),
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
