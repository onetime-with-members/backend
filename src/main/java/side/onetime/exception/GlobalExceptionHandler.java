package side.onetime.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.dto.ErrorReasonDto;
import side.onetime.global.common.status.ErrorStatus;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleCustomException(CustomException e) {
        if (e.getHttpStatus().is5xxServerError()) {
            log.error("CustomException (5xx): {}", e.getMessage(), e);
        } else {
            logError("CustomException", e);
        }
        return ApiResponse.onFailure(e.getErrorCode());
    }

    // Security 인증 관련 처리
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleSecurityException(SecurityException e) {
        logError(e.getMessage(), e);
        return ApiResponse.onFailure(ErrorStatus._UNAUTHORIZED);
    }

    // Security 인가 관련 처리 (메서드 레벨 @PreAuthorize 등에서 권한 부족 시)
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleAuthorizationDeniedException(
            org.springframework.security.authorization.AuthorizationDeniedException e) {
        logError("AuthorizationDeniedException", e);
        return ApiResponse.onFailure(ErrorStatus._FORBIDDEN);
    }

    // IllegalArgumentException 처리 (잘못된 인자가 전달된 경우)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        String errorMessage = "잘못된 요청입니다: " + e.getMessage();
        logError("IllegalArgumentException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // ConstraintViolationException 처리 (쿼리 파라미터에 올바른 값이 들어오지 않은 경우)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleValidationParameterError(ConstraintViolationException ex) {
        String errorMessage = ex.getMessage();
        logError("ConstraintViolationException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // MissingServletRequestParameterException 처리 (필수 쿼리 파라미터가 입력되지 않은 경우)
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        String errorMessage = "필수 파라미터 '" + ex.getParameterName() + "'가 없습니다.";
        logError("MissingServletRequestParameterException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // MethodArgumentNotValidException 처리 (RequestBody로 들어온 필드들의 유효성 검증에 실패한 경우)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String combinedErrors = extractFieldErrors(ex.getBindingResult().getFieldErrors());
        logError("Validation error", combinedErrors);
        return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, combinedErrors);
    }

    // NoHandlerFoundException 처리 (요청 경로에 매핑된 핸들러가 없는 경우)
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        String errorMessage = "해당 경로에 대한 핸들러를 찾을 수 없습니다: " + ex.getRequestURL();
        logError("NoHandlerFoundException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._NOT_FOUND_HANDLER, errorMessage);
    }

    // HttpRequestMethodNotSupportedException 처리 (지원하지 않는 HTTP 메소드 요청이 들어온 경우)
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        String errorMessage = "지원하지 않는 HTTP 메소드 요청입니다: " + ex.getMethod();
        logError("HttpRequestMethodNotSupportedException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._METHOD_NOT_ALLOWED, errorMessage);
    }

    // HttpMediaTypeNotSupportedException 처리 (지원하지 않는 미디어 타입 요청이 들어온 경우)
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        String errorMessage = "지원하지 않는 미디어 타입입니다: " + ex.getContentType();
        logError("HttpMediaTypeNotSupportedException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._UNSUPPORTED_MEDIA_TYPE, errorMessage);
    }

    // Jackson 역직렬화 중 enum 값이 유효하지 않은 경우 처리
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException formatException) {
            if (formatException.getTargetType().isEnum()) {
                String errorMessage = "올바르지 않은 enum 값입니다. 허용되지 않은 값: " + formatException.getValue();
                logError("Invalid enum value", errorMessage);
                return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, errorMessage);
            }
        }
        String errorMessage = "요청 본문이 잘못되었습니다.";
        logError("HttpMessageNotReadableException", errorMessage);
        return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // 내부 서버 에러 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleException(Exception e) {
        // 서버 내부 에러 발생 시 로그에 예외 내용 기록
        log.error("🚨 Internal Server Error: {}", e.getMessage(), e);
        return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
    }

    // 유효성 검증 오류 메시지 추출 메서드 (FieldErrors)
    private String extractFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }

    // 로그 기록 메서드
    private void logError(String message, Object errorDetails) {
        // 예외 객체가 Throwable이면 stack trace 출력 없이 메시지만
        if (errorDetails instanceof Throwable t) {
            log.error("{}: {}", message, t.getMessage());
        } else {
            log.error("{}: {}", message, errorDetails);
        }
    }
}
