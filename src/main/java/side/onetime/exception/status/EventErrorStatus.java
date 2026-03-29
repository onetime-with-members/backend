package side.onetime.exception.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import side.onetime.global.common.code.BaseErrorCode;
import side.onetime.global.common.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum EventErrorStatus implements BaseErrorCode {
    _NOT_FOUND_EVENT(HttpStatus.NOT_FOUND, "EVENT-001", "이벤트를 찾을 수 없습니다."),
    _IS_NOT_DATE_FORMAT(HttpStatus.BAD_REQUEST, "EVENT-002", "날짜 이벤트에 요일을 입력할 수 없습니다."),
    _IS_NOT_DAY_FORMAT(HttpStatus.BAD_REQUEST, "EVENT-003", "요일 이벤트에 날짜를 입력할 수 없습니다."),
    _NOT_FOUND_EVENT_QR_CODE(HttpStatus.NOT_FOUND, "EVENT-004", "이벤트 QR 코드를 찾을 수 없습니다."),
    _FAILED_GENERATE_QR_CODE(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT-005", "QR 코드를 생성하고 업로드 하는 과정에서 문제가 발생했습니다."),
    _INVALID_CONFIRMATION_REQUEST(HttpStatus.BAD_REQUEST, "EVENT-006", "유효하지 않은 확정 요청입니다."),
    _CANNOT_MODIFY_CONFIRMED_EVENT(HttpStatus.CONFLICT, "EVENT-007", "확정된 이벤트는 수정할 수 없습니다."),
    _NOT_FOUND_EVENT_CONFIRMATION(HttpStatus.NOT_FOUND, "EVENT-008", "확정된 이벤트 정보를 찾을 수 없습니다."),
    _FAILED_SERIALIZE_KAKAO_EVENT(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT-009", "카카오 일정 정보를 처리하는 중 오류가 발생했습니다."),
    _KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT-010", "카카오 토큰 발급에 실패했습니다."),
    _KAKAO_CALENDAR_API_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT-011", "카카오 톡캘린더 API 호출에 실패했습니다 ."),
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
