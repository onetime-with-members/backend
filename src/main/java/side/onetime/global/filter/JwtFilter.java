package side.onetime.global.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.auth.service.CustomAdminDetailsService;
import side.onetime.auth.service.CustomUserDetailsService;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.service.TokenService;
import side.onetime.util.ClientInfoExtractor;
import side.onetime.util.CookieUtil;
import side.onetime.util.JwtUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAdminDetailsService customAdminDetailsService;
    private final TokenService tokenService;
    private final ClientInfoExtractor clientInfoExtractor;

    /**
     * 요청을 처리하며 JWT 검증 및 인증 설정을 수행합니다.
     *
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @param filterChain  필터 체인 객체
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException      입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String refreshToken = null;
        boolean isAdminRequest = false;

        // 1. API 요청의 Authorization 헤더에서 토큰 추출
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = jwtUtil.getTokenFromHeader(authorizationHeader);
        }

        // 2. 어드민 요청(페이지 + API)의 쿠키에서 토큰 추출
        String requestUri = request.getRequestURI();
        if (token == null && (requestUri.startsWith("/admin") || requestUri.startsWith("/api/v1/admin"))) {
            var adminAccessToken = CookieUtil.getAdminAccessToken(request);
            if (adminAccessToken.isPresent()) {
                token = adminAccessToken.get();
                isAdminRequest = true;
                refreshToken = CookieUtil.getAdminRefreshToken(request).orElse(null);
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtUtil.validateToken(token);
            authenticateUser(token);
            filterChain.doFilter(request, response);

        } catch (CustomException e) {
            // 어드민 요청이고 리프레시 토큰이 있으면 자동 재발급 시도
            if (isAdminRequest && refreshToken != null) {
                try {
                    String userIp = clientInfoExtractor.extractClientIp(request);
                    String userAgent = clientInfoExtractor.extractUserAgent(request);
                    ReissueTokenResponse reissued = tokenService.reissueToken(
                            new ReissueTokenRequest(refreshToken), userIp, userAgent
                    );

                    // 새 토큰으로 쿠키 갱신
                    CookieUtil.setAdminTokenCookies(request, response, reissued.accessToken(), reissued.refreshToken());

                    // 새 액세스 토큰으로 인증
                    authenticateUser(reissued.accessToken());
                    filterChain.doFilter(request, response);
                    return;

                } catch (Exception reissueEx) {
                    log.warn("[Admin] 토큰 재발급 실패 - 사유: {}", reissueEx.getMessage());
                    // 재발급 실패 시 로그인 페이지로 리다이렉트
                    response.sendRedirect("/admin/login");
                    return;
                }
            }

            log.error("JWT 필터 예외 발생 - 요청 URI: {}, 메서드: {}", request.getRequestURI(), request.getMethod());
            writeErrorResponse(response, e);
        }
    }

    /**
     * 토큰에서 사용자 정보를 추출하여 인증 설정
     */
    private void authenticateUser(String token) {
        String userType = jwtUtil.getClaimFromToken(token, "userType", String.class);
        Long userId = jwtUtil.getClaimFromToken(token, "userId", Long.class);

        UserDetails userDetails = "ADMIN".equals(userType)
                ? customAdminDetailsService.loadAdminByAdminId(userId)
                : customUserDetailsService.loadUserByUserId(userId);
        setAuthentication(userDetails);
    }

    /**
     * 인증 정보를 SecurityContext에 설정합니다.
     *
     * @param userDetails 인증된 사용자
     */
    private void setAuthentication(UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 특정 경로에 대해 JWT Filter를 생략합니다.
     *
     * @param request HTTP 요청 객체
     * @return true일 경우 해당 요청에 대해 필터를 적용하지 않음
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/actuator/health") ||
                path.equals("/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/favicon.ico") ||
                path.equals("/api/v1/tokens/action-reissue") ||
                path.equals("/api/v1/users/logout") ||
                path.equals("/api/v1/admin/action-reissue") ||
                path.equals("/admin/login") ||
                path.startsWith("/admin/css") ||
                path.startsWith("/admin/js") ||
                path.startsWith("/admin/vendor");
    }

    /**
     * JWT 검증 중 발생한 CustomException을 클라이언트에게 응답 형태로 반환합니다.
     *
     * @param response HTTP 응답 객체
     * @param e        JWT 검증 중 발생한 CustomException
     * @throws IOException 출력 스트림 처리 중 오류 발생 시
     */
    private void writeErrorResponse(HttpServletResponse response, CustomException e) throws IOException {
        int status = e.getErrorCode().getReasonHttpStatus().getHttpStatus().value();
        String code = e.getErrorCode().getReasonHttpStatus().getCode();
        String message = e.getErrorCode().getReasonHttpStatus().getMessage();

        log.error("❌ JWT 예외 발생 - status: {}, code: {}, message: {}", status, code, message);

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{"
                        + "\"is_success\": false,"
                        + "\"code\": \"" + code + "\","
                        + "\"message\": \"" + message + "\","
                        + "\"payload\": null"
                        + "}"
        );
    }
}
