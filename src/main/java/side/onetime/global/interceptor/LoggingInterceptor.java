package side.onetime.global.interceptor;

import static net.logstash.logback.argument.StructuredArguments.*;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

	private static final int HTTP_ERROR_THRESHOLD = 400;

    /**
     * 요청 전 처리 로직을 수행합니다.
     */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		request.setAttribute("startTime", System.currentTimeMillis());
		return true;
	}

    /**
     * 요청 완료 후 처리 로직을 수행합니다.
     */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		Long start = Optional.ofNullable(request.getAttribute("startTime"))
			.map(Long.class::cast)
			.orElse(0L);
		long duration = System.currentTimeMillis() - start;
		int status = response.getStatus();
		String method = request.getMethod();
		String uri = request.getRequestURI();

		if (status >= HTTP_ERROR_THRESHOLD) {
			log.error("❌ Request failed",
				kv("http_method", method),
				kv("request_uri", uri),
				kv("http_status", status),
				kv("duration_ms", duration)
			);
		} else {
			log.info("✅ Request completed successfully",
				kv("http_method", method),
				kv("request_uri", uri),
				kv("http_status", status),
				kv("duration_ms", duration)
			);
		}
	}
}
