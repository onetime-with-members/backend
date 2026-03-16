package side.onetime.util;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 쿠키 관리 유틸리티
 */
public class CookieUtil {

    private CookieUtil() {
        throw new AssertionError();
    }

    // Admin Cookie Constants
    public static final String ADMIN_ACCESS_TOKEN_COOKIE = "admin_access_token";
    public static final String ADMIN_REFRESH_TOKEN_COOKIE = "admin_refresh_token";
    public static final int ADMIN_ACCESS_COOKIE_MAX_AGE = 60 * 60; // 1 hour
    public static final int ADMIN_REFRESH_COOKIE_MAX_AGE = 60 * 60 * 24 * 14; // 14 days

    /**
     * 쿠키 값 조회
     */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue);
    }

    /**
     * 쿠키 설정 (Secure, SameSite 지원)
     *
     * @param request  요청 객체 (HTTPS 여부 감지용)
     * @param response 응답 객체
     * @param name     쿠키 이름
     * @param value    쿠키 값
     * @param maxAge   만료 시간 (초)
     */
    public static void setCookie(HttpServletRequest request, HttpServletResponse response,
                                 String name, String value, int maxAge) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value);
        cookie.append("; Path=/");
        cookie.append("; Max-Age=").append(maxAge);
        cookie.append("; HttpOnly");
        cookie.append("; Secure");
        cookie.append("; SameSite=Lax");

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 쿠키 삭제 (Secure, SameSite 지원)
     *
     * @param request  요청 객체
     * @param response 응답 객체
     * @param name     쿠키 이름
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=");
        cookie.append("; Path=/");
        cookie.append("; Max-Age=0");
        cookie.append("; HttpOnly");
        cookie.append("; Secure");
        cookie.append("; SameSite=Lax");

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // ==================== Admin Cookie Methods ====================

    /**
     * 어드민 액세스 토큰 쿠키 조회
     */
    public static Optional<String> getAdminAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ADMIN_ACCESS_TOKEN_COOKIE);
    }

    /**
     * 어드민 리프레시 토큰 쿠키 조회
     */
    public static Optional<String> getAdminRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, ADMIN_REFRESH_TOKEN_COOKIE);
    }

    /**
     * 어드민 토큰 쿠키 설정
     */
    public static void setAdminTokenCookies(HttpServletRequest request, HttpServletResponse response,
                                            String accessToken, String refreshToken) {
        setCookie(request, response, ADMIN_ACCESS_TOKEN_COOKIE, accessToken, ADMIN_ACCESS_COOKIE_MAX_AGE);
        setCookie(request, response, ADMIN_REFRESH_TOKEN_COOKIE, refreshToken, ADMIN_REFRESH_COOKIE_MAX_AGE);
    }

    /**
     * 어드민 토큰 쿠키 삭제
     */
    public static void clearAdminTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, ADMIN_ACCESS_TOKEN_COOKIE);
        deleteCookie(request, response, ADMIN_REFRESH_TOKEN_COOKIE);
    }
}
