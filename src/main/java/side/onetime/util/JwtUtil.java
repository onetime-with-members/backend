package side.onetime.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import side.onetime.domain.User;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.repository.UserRepository;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;
    private final UserRepository userRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 액세스 토큰을 발급하는 메서드
    public String generateAccessToken(Long userId, long expirationMillis) {
        log.info("액세스 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("userId", userId.toString()) // 클레임에 userId 추가
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    // 레지스터 토큰을 발급하는 메서드
    public String generateRegisterToken(String provider, String providerId, String name, String email, long expirationMillis) {
        log.info("레지스터 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("provider", provider)     // 클레임에 provider 추가
                .claim("providerId", providerId) // 클레임에 providerId 추가
                .claim("name", name)             // 클레임에 name 추가
                .claim("email", email)           // 클레임에 email 추가
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    // 리프레쉬 토큰을 발급하는 메서드
    public String generateRefreshToken(Long userId, long expirationMillis) {
        log.info("리프레쉬 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("userId", userId.toString()) // 클레임에 userId 추가
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    // 응답 헤더에서 액세스 토큰을 반환하는 메서드
    public String getTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    // 토큰에서 유저 id를 반환하는 메서드
    public Long getUserIdFromToken(String token) {
        try {
            validateTokenExpiration(token);
            String userId = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("userId", String.class);
            log.info("유저 id를 반환합니다.");
            return Long.parseLong(userId);
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("토큰에서 userId를 반환하던 도중 에러가 발생했습니다.");
            throw new CustomException(TokenErrorStatus._INVALID_TOKEN);
        }
    }

    // 헤더에서 유저를 반환하는 메서드
    public User getUserFromHeader(String authorizationHeader) {
        String token = getTokenFromHeader(authorizationHeader);
        validateTokenExpiration(token);

        return userRepository.findById(getUserIdFromToken(token))
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
    }

    // 토큰에서 provider를 반환하는 메서드
    public String getProviderFromToken(String token) {
        try {
            validateTokenExpiration(token);
            String userId = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("provider", String.class);
            log.info("provider를 반환합니다.");
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("토큰에서 provider를 반환하는 도중 에러가 발생했습니다.");
            throw new CustomException(TokenErrorStatus._INVALID_TOKEN);
        }
    }

    // 토큰에서 providerId를 반환하는 메서드
    public String getProviderIdFromToken(String token) {
        try {
            validateTokenExpiration(token);
            String providerId = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("providerId", String.class);
            log.info("providerId를 반환합니다.");
            return providerId;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("토큰에서 providerId를 반환하는 도중 에러가 발생했습니다.");
            throw new CustomException(TokenErrorStatus._INVALID_TOKEN);
        }
    }

    // 토큰에서 이름을 반환하는 메서드
    public String getNameFromToken(String token) {
        try {
            validateTokenExpiration(token);
            String name = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("name", String.class);
            log.info("name을 반환합니다.");
            return name;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("토큰에서 이름을 반환하는 도중 에러가 발생했습니다.");
            throw new CustomException(TokenErrorStatus._INVALID_TOKEN);
        }
    }

    // 토큰에서 이메일을 반환하는 메서드
    public String getEmailFromToken(String token) {
        try {
            validateTokenExpiration(token);
            String email = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("email", String.class);
            log.info("email을 반환합니다.");
            return email;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("토큰에서 이메일을 반환하는 도중 에러가 발생했습니다.");
            throw new CustomException(TokenErrorStatus._INVALID_TOKEN);
        }
    }

    // Jwt 토큰의 유효기간을 확인하는 메서드
    public void validateTokenExpiration(String token) {
        try {
            log.info("토큰의 유효기간을 확인합니다.");
            Date expirationDate = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.error("만료된 토큰입니다.");
            throw new CustomException(TokenErrorStatus._EXPIRED_TOKEN);
        }
    }
}