package side.onetime.util;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트 정보 추출 유틸리티
 *
 * HttpServletRequest에서 IP 주소, User-Agent 등의 클라이언트 정보를 추출
 */
@Component
public class ClientInfoExtractor {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 클라이언트 IP 주소 추출
     *
     * 프록시/로드밸런서 환경을 고려하여 X-Forwarded-For 등의 헤더 확인 후
     * 없으면 remoteAddr 반환
     *
     * @param request HttpServletRequest
     * @return 클라이언트 IP 주소
     */
    public String extractClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * User-Agent 추출
     *
     * @param request HttpServletRequest
     * @return User-Agent 문자열 (최대 512자)
     */
    public String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
    }
}
