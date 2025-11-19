package side.onetime.exception.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import side.onetime.global.common.code.BaseErrorCode;
import side.onetime.global.common.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum FixedErrorStatus implements BaseErrorCode {
	_NOT_FOUND_FIXED_SCHEDULES(HttpStatus.NOT_FOUND, "FIXED-001", "고정 스케줄 목록을 가져오는 데 실패했습니다."),
	_NOT_FOUND_EVERYTIME_TIMETABLE(HttpStatus.NOT_FOUND, "FIXED-002", "에브리타임 시간표를 가져오는 데 실패했습니다. 공개 범위를 확인해주세요."),
	_EVERYTIME_TIMETABLE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FIXED-003", "에브리타임 시간표 파싱 중 문제가 발생했습니다."),
	_EVERYTIME_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "FIXED-004", "에브리타임 API 연동 중 서버 오류가 발생했습니다.")
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
