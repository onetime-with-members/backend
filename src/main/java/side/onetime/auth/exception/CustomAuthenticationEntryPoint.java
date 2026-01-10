package side.onetime.auth.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import side.onetime.global.common.status.ErrorStatus;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 로그인을 하지 않고 접근할 떄 발생한 authException을 클라이언트에게 응답 형태로 반환합니다.
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param authException 로그인 없이 로그인이 필요한 리소스에 접근하여 발생한 exception
     * @throws IOException 출력 스트림 처리 중 오류 발생 시
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("❌ 인증되지 않은 접근 - 요청 URI: {}, 메서드: {}", request.getRequestURI(), request.getMethod());

        ErrorStatus status = ErrorStatus._UNAUTHORIZED;

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
