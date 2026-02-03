package side.onetime.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.dto.admin.request.LoginAdminUserRequest;
import side.onetime.dto.admin.response.LoginAdminUserResponse;
import side.onetime.exception.CustomException;
import side.onetime.service.AdminService;
import side.onetime.service.StatisticsService;
import side.onetime.util.ClientInfoExtractor;
import side.onetime.util.CookieUtil;
import side.onetime.util.DateUtil;
import side.onetime.util.JwtUtil;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;
    private final StatisticsService statisticsService;
    private final JwtUtil jwtUtil;
    private final ClientInfoExtractor clientInfoExtractor;

    // ==================== Model Attributes ====================

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
            log.debug("Could not get admin name: {}", e.getMessage());
        }
        return "A";
    }

    // ==================== Authentication ====================

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

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

            CookieUtil.setAdminTokenCookies(response, result.accessToken(), result.refreshToken());

            return "redirect:/admin/dashboard";
        } catch (CustomException e) {
            log.warn("Admin login failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        CookieUtil.clearAdminTokenCookies(response);
        return "redirect:/admin/login";
    }

    // ==================== Dashboard ====================

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

    // ==================== Statistics Pages ====================

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

    // ==================== Email Page ====================

    @GetMapping("/email")
    public String emailPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "email");
        model.addAttribute("pageTitle", "Send Email");
        return "admin/email";
    }

}
