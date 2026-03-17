package side.onetime.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.dto.admin.request.LoginAdminUserRequest;
import side.onetime.dto.admin.response.LoginAdminUserResponse;
import side.onetime.exception.CustomException;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.service.AdminService;
import side.onetime.service.StatisticsService;
import side.onetime.util.ClientInfoExtractor;
import side.onetime.util.CookieUtil;
import side.onetime.util.DateUtil;
import side.onetime.util.JwtUtil;

/**
 * 어드민 페이지 컨트롤러 (SSR)
 * Thymeleaf 템플릿 렌더링을 담당
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;
    private final StatisticsService statisticsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final ClientInfoExtractor clientInfoExtractor;

    /**
     * 현재 로그인된 어드민의 이름(이메일 첫 글자) 반환
     * 모든 페이지에서 공통으로 사용
     */
    @ModelAttribute("adminName")
    public String getAdminName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails details) {
                String email = details.admin().getEmail();
                if (email != null && !email.isEmpty()) {
                    return String.valueOf(email.charAt(0)).toUpperCase();
                }
            }
        } catch (Exception e) {
            log.debug("[Admin] 어드민 이름 조회 실패 - 사유: {}", e.getMessage());
        }
        return "A";
    }

    /**
     * 로그인 페이지 렌더링
     */

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * 로그인 처리
     * 성공 시 JWT 토큰을 쿠키에 저장하고 대시보드로 리다이렉트
     */

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            String browserId = jwtUtil.hashUserAgent(request.getHeader("User-Agent"));
            String userIp = clientInfoExtractor.extractClientIp(request);
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            LoginAdminUserResponse result = adminService.loginAdminUser(
                    new LoginAdminUserRequest(email, password), browserId, userIp, userAgent
            );

            CookieUtil.setAdminTokenCookies(request, response, result.accessToken(), result.refreshToken());

            return "redirect:/admin/dashboard";
        } catch (CustomException e) {
            log.warn("[Admin] 로그인 실패 - 사유: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
        }
    }

    /**
     * 로그아웃 처리
     * 서버 측 refresh token revoke + 쿠키 삭제 후 로그인 페이지로 리다이렉트
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String accessToken = CookieUtil.extractAdminAccessToken(request);
            if (accessToken != null) {
                Long adminId = jwtUtil.getClaimFromToken(accessToken, "userId", Long.class);
                refreshTokenRepository.revokeAllByUserId(adminId, "ADMIN");
            }
        } catch (Exception e) {
            log.debug("[Admin] 로그아웃 시 토큰 revoke 실패 - 사유: {}", e.getMessage());
        }
        CookieUtil.clearAdminTokenCookies(request, response);
        return "redirect:/admin/login";
    }

    /**
     * 대시보드 페이지 렌더링
     * KPI 요약 및 차트 데이터 포함
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);

        model.addAttribute("startDate", DateUtil.formatToIsoDate(dates[0]));
        model.addAttribute("endDate", DateUtil.formatToIsoDate(dates[1]));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("summary", statisticsService.getDashboardSummary(dates[0], dates[1]));
        model.addAttribute("charts", statisticsService.getDashboardCharts(dates[0], dates[1]));
        return "admin/dashboard";
    }

    /**
     * 유저 통계 페이지 렌더링
     */
    @GetMapping("/statistics/users")
    public String usersStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);

        model.addAttribute("startDate", DateUtil.formatToIsoDate(dates[0]));
        model.addAttribute("endDate", DateUtil.formatToIsoDate(dates[1]));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "users");
        model.addAttribute("pageTitle", "User Statistics");
        model.addAttribute("data", statisticsService.getUserStatistics(dates[0], dates[1]));
        return "admin/users";
    }

    /**
     * 이벤트 통계 페이지 렌더링
     */
    @GetMapping("/statistics/events")
    public String eventsStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);

        model.addAttribute("startDate", DateUtil.formatToIsoDate(dates[0]));
        model.addAttribute("endDate", DateUtil.formatToIsoDate(dates[1]));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "events");
        model.addAttribute("pageTitle", "Event Statistics");
        model.addAttribute("data", statisticsService.getEventStatistics(dates[0], dates[1]));
        return "admin/events";
    }

    /**
     * 리텐션 분석 페이지 렌더링
     */
    @GetMapping("/statistics/retention")
    public String retentionStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);

        model.addAttribute("startDate", DateUtil.formatToIsoDate(dates[0]));
        model.addAttribute("endDate", DateUtil.formatToIsoDate(dates[1]));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "retention");
        model.addAttribute("pageTitle", "Retention Analysis");
        model.addAttribute("data", statisticsService.getRetentionStatistics(dates[0], dates[1]));
        return "admin/retention";
    }

    /**
     * 마케팅 타겟 페이지 렌더링
     */
    @GetMapping("/statistics/marketing")
    public String marketingTargets(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);

        model.addAttribute("startDate", DateUtil.formatToIsoDate(dates[0]));
        model.addAttribute("endDate", DateUtil.formatToIsoDate(dates[1]));
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "marketing");
        model.addAttribute("pageTitle", "Marketing Targets");
        model.addAttribute("data", statisticsService.getMarketingTargets(dates[0], dates[1]));
        return "admin/marketing";
    }

    /**
     * 이메일 발송 페이지 렌더링
     */
    @GetMapping("/email")
    public String emailPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "email");
        model.addAttribute("pageTitle", "Send Email");
        return "admin/email";
    }

    /**
     * 배너 관리 페이지 렌더링
     */
    @GetMapping("/banner")
    public String bannerPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "banner");
        model.addAttribute("pageTitle", "Banner Management");
        return "admin/banner";
    }
}
