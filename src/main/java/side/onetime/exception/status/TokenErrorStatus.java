package side.onetime.exception.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import side.onetime.global.common.code.BaseErrorCode;
import side.onetime.global.common.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum TokenErrorStatus implements BaseErrorCode {
    _TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN-001", "JWT 서명이 유효하지 않습니다."),
    _TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "TOKEN-002", "지원되지 않는 JWT 토큰입니다."),
    _TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN-003", "만료된 토큰입니다."),
    _TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "TOKEN-004", "잘못된 JWT 토큰입니다."),
    _NOT_FOUND_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN-005", "리프레쉬 토큰을 찾을 수 없습니다."),
    _TOKEN_CLAIM_EXTRACTION_ERROR(HttpStatus.UNAUTHORIZED, "TOKEN-006", "토큰에서 claim 값을 추출하던 도중 에러가 발생했습니다."),
    _INVALID_USER_TYPE(HttpStatus.BAD_REQUEST, "TOKEN-007", "알 수 없는 타입의 액세스 토큰이 발행되었습니다."),
    _NOT_FOUND_HEADER(HttpStatus.BAD_REQUEST, "TOKEN-008", "Authorization 헤더가 존재하지 않거나 형식이 잘못되었습니다."),
    _TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOKEN-009", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
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
