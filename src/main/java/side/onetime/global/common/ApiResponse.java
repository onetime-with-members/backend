package side.onetime.global.common;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import side.onetime.global.common.code.BaseCode;
import side.onetime.global.common.code.BaseErrorCode;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
    @JsonProperty("is_success")
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    private final T payload;
    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code, T payload) {
        ApiResponse<T> response = new ApiResponse<>(true, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), payload);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code) {
        ApiResponse<T> response = new ApiResponse<>(true, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode code) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    public static <T> ResponseEntity<Object> onFailure(BaseErrorCode code, String message) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), message, null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }
}
