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
    _EMAIL_SQS_PUBLISH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-003", "이메일 큐 발행에 실패했습니다."),
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
