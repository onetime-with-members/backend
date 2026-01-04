package side.onetime.auth.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import side.onetime.global.common.status.ErrorStatus;

import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 접근할 수 있는 권한(Role)이 없을 때 발생한 accessDeniedException을 클라이언트에게 응답 형태로 반환합니다.
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param accessDeniedException 접근 권한이 없어 발생한 exception
     * @throws IOException 출력 스트림 처리 중 오류 발생 시
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "ANONYMOUS";
        log.error("❌ 금지된 접근 - 사용자: {}, 요청 URI: {}, 메서드: {}", username, request.getRequestURI(), request.getMethod());

        ErrorStatus status = ErrorStatus._FORBIDDEN;

        response.setStatus(status.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{"
                        + "\"is_success\": false,"
                        + "\"code\": \"" + status.getCode() + "\","
                        + "\"message\": \"" + status.getMessage() + "\","
                        + "\"payload\": null"
                        + "}"
        );
    }
}
