package side.onetime.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

/**
 * 쿠키 관리 유틸리티
 */
public class CookieUtil {

    private CookieUtil() {
        throw new AssertionError();
    }

    // Admin Cookie Constants
    public static final String ADMIN_ACCESS_TOKEN_COOKIE = "admin_token";
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
     * 쿠키 설정
     */
    public static void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
    public static void setAdminTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setCookie(response, ADMIN_ACCESS_TOKEN_COOKIE, accessToken, ADMIN_ACCESS_COOKIE_MAX_AGE);
        setCookie(response, ADMIN_REFRESH_TOKEN_COOKIE, refreshToken, ADMIN_REFRESH_COOKIE_MAX_AGE);
    }

    /**
     * 어드민 토큰 쿠키 삭제
     */
    public static void clearAdminTokenCookies(HttpServletResponse response) {
        deleteCookie(response, ADMIN_ACCESS_TOKEN_COOKIE);
        deleteCookie(response, ADMIN_REFRESH_TOKEN_COOKIE);
    }
}
