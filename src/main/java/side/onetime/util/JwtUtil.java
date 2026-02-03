package side.onetime.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.User;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME; // 액세스 토큰 유효기간

    @Value("${jwt.admin-user-access-token.expiration-time}")
    private long ADMIN_USER_ACCESS_TOKEN_EXPIRATION_TIME; // 어드민 유저 액세스 토큰 유효기간

    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME; // 리프레쉬 토큰 유효기간

    @Value("${jwt.register-token.expiration-time}")
    private long REGISTER_TOKEN_EXPIRATION_TIME; // 레지스터 토큰 유효기간

    @Value("${jwt.browser-id-salt}")
    private String browserIdSalt;

    private final UserRepository userRepository;

    /**
     * JWT 서명 키를 생성 및 반환.
     *
     * @return SecretKey 객체
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 액세스 토큰 생성 메서드.
     *
     * @param userId 유저 ID
     * @param userType 유저 타입 (예: "USER" 또는 "ADMIN")
     * @return 생성된 액세스 토큰
     */
    public String generateAccessToken(Long userId, String userType) {
        long expirationMillis;

        switch (userType.toUpperCase()) {
            case "ADMIN" -> {
                expirationMillis = ADMIN_USER_ACCESS_TOKEN_EXPIRATION_TIME;
            }
            case "USER" -> {
                expirationMillis = ACCESS_TOKEN_EXPIRATION_TIME;
            }
            default -> {
                throw new CustomException(TokenErrorStatus._INVALID_USER_TYPE);
            }
        }

        return Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType.toUpperCase())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * 만료된 액세스 토큰 생성 메서드 (테스트 전용).
     *
     * @param userId 유저 ID
     * @param userType 유저 타입
     * @return 이미 만료된 액세스 토큰
     */
    public String generateExpiredAccessToken(Long userId, String userType) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType.toUpperCase())
                .issuedAt(new Date(System.currentTimeMillis() - 2000L))
                .expiration(new Date(System.currentTimeMillis() - 1000L))
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * 레지스터 토큰 생성 메서드.
     *
     * @param provider 제공자
     * @param providerId 제공자 ID
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @return 생성된 레지스터 토큰
     */
    public String generateRegisterToken(String provider, String providerId, String name, String email, String browserId) {
        return Jwts.builder()
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("name", name)
                .claim("email", email)
                .claim("browserId", browserId)
                .claim("type", "REGISTER_TOKEN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REGISTER_TOKEN_EXPIRATION_TIME))
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * 리프레시 토큰 생성 메서드.
     *
     * @param userId 유저 ID
     * @param userType 유저 타입 (USER, ADMIN)
     * @param browserId 브라우저 식별값 (User-Agent 기반 해시)
     * @param jti JWT 고유 식별자 (Token Rotation 추적용)
     * @return 생성된 리프레시 토큰
     */
    public String generateRefreshToken(Long userId, String userType, String browserId, String jti) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType.toUpperCase())
                .claim("browserId", browserId)
                .claim("jti", jti)
                .claim("type", "REFRESH_TOKEN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * 리프레시 토큰 만료 시간 반환 (밀리초)
     *
     * @return 리프레시 토큰 만료 시간 (ms)
     */
    public long getRefreshTokenExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION_TIME;
    }

    /**
     * 리프레시 토큰 만료 시각 계산
     *
     * @param issuedAt 발급 시각
     * @return 만료 시각 (issuedAt + REFRESH_TOKEN_EXPIRATION_TIME)
     */
    public LocalDateTime calculateRefreshTokenExpiryAt(LocalDateTime issuedAt) {
        return issuedAt.plusSeconds(REFRESH_TOKEN_EXPIRATION_TIME / 1000);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출.
     *
     * @param authorizationHeader Authorization 헤더
     * @return 토큰 문자열
     */
    public String getTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new CustomException(TokenErrorStatus._NOT_FOUND_HEADER);
        }
        return authorizationHeader.substring(7);
    }

    /**
     * 토큰에서 특정 클레임 값 추출.
     *
     * @param token JWT 토큰
     * @param key   클레임 키
     * @param clazz 반환할 값의 클래스 타입
     * @param <T>   반환할 값의 타입
     * @return 클레임 값
     */
    public <T> T getClaimFromToken(String token, String key, Class<T> clazz) {
        try {
            return Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(key, clazz);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(TokenErrorStatus._TOKEN_CLAIM_EXTRACTION_ERROR);
        }
    }

    /**
     * 헤더에서 유저 객체 반환.
     *
     * @param authorizationHeader Authorization 헤더
     * @return 유저 객체
     */
    public User getUserFromHeader(String authorizationHeader) {
        String token = getTokenFromHeader(authorizationHeader);
        validateToken(token);

        return userRepository.findById(getClaimFromToken(token, "userId", Long.class))
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
    }

    /**
     * JWT 토큰 검증.
     *
     * @param token JWT 토큰
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token);
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new CustomException(TokenErrorStatus._TOKEN_SIGNATURE_INVALID);
        } catch (ExpiredJwtException e) {
            throw new CustomException(TokenErrorStatus._TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(TokenErrorStatus._TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            throw new CustomException(TokenErrorStatus._TOKEN_MALFORMED);
        }
    }

    /**
     * User-Agent + Salt 해시 처리 메서드.
     *
     * @param userAgent 브라우저의 User-Agent 문자열
     * @return 해시된 브라우저 ID
     */
    public String hashUserAgent(String userAgent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = userAgent + browserIdSalt;
            byte[] hash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 해싱 실패", e);
        }
    }

    // ==================== Admin Cookie Handling ====================

    public static final String ADMIN_ACCESS_TOKEN_COOKIE = "admin_token";
    public static final String ADMIN_REFRESH_TOKEN_COOKIE = "admin_refresh_token";
    public static final int ADMIN_ACCESS_COOKIE_MAX_AGE = 60 * 60; // 1 hour
    public static final int ADMIN_REFRESH_COOKIE_MAX_AGE = 60 * 60 * 24 * 14; // 14 days

    /**
     * 어드민 액세스 토큰 쿠키 조회
     */
    public Optional<String> getAdminAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ADMIN_ACCESS_TOKEN_COOKIE);
    }

    /**
     * 어드민 리프레시 토큰 쿠키 조회
     */
    public Optional<String> getAdminRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, ADMIN_REFRESH_TOKEN_COOKIE);
    }

    /**
     * 어드민 토큰 쿠키 설정
     */
    public void setAdminTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie(ADMIN_ACCESS_TOKEN_COOKIE, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(ADMIN_ACCESS_COOKIE_MAX_AGE);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(ADMIN_REFRESH_TOKEN_COOKIE, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(ADMIN_REFRESH_COOKIE_MAX_AGE);
        response.addCookie(refreshCookie);
    }

    /**
     * 어드민 토큰 쿠키 삭제
     */
    public void clearAdminTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(ADMIN_ACCESS_TOKEN_COOKIE, null);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(ADMIN_REFRESH_TOKEN_COOKIE, null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue);
    }
}
