package side.onetime.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.dto.admin.request.LoginAdminUserRequest;
import side.onetime.dto.admin.response.LoginAdminUserResponse;
import side.onetime.exception.CustomException;
import side.onetime.service.AdminService;
import side.onetime.service.StatisticsService;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;
    private final StatisticsService statisticsService;

    private static final String ADMIN_TOKEN_COOKIE = "admin_token";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24; // 1 day

    // ==================== Authentication ====================

    /**
     * Admin login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * Process admin login
     */
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            LoginAdminUserResponse result = adminService.loginAdminUser(
                    new LoginAdminUserRequest(email, password)
            );

            // Store JWT in HttpOnly cookie
            Cookie cookie = new Cookie(ADMIN_TOKEN_COOKIE, result.accessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/admin");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            response.addCookie(cookie);

            return "redirect:/admin/dashboard";
        } catch (CustomException e) {
            log.warn("Admin login failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
        }
    }

    /**
     * Admin logout
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        // Delete cookie
        Cookie cookie = new Cookie(ADMIN_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/admin");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/admin/login";
    }

    // ==================== Dashboard ====================

    /**
     * Main dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = resolveDateRange(startDate, endDate);
        addDateRangeToModel(model, dates[0], dates[1]);

        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("summary", statisticsService.getDashboardSummary(dates[0], dates[1]));
        model.addAttribute("charts", statisticsService.getDashboardCharts(dates[0], dates[1]));
        return "admin/dashboard";
    }

    // ==================== Statistics Pages ====================

    /**
     * User statistics page
     */
    @GetMapping("/statistics/users")
    public String usersStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = resolveDateRange(startDate, endDate);
        addDateRangeToModel(model, dates[0], dates[1]);

        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "users");
        model.addAttribute("pageTitle", "User Statistics");
        model.addAttribute("data", statisticsService.getUserStatistics(dates[0], dates[1]));
        return "admin/users";
    }

    /**
     * Event statistics page
     */
    @GetMapping("/statistics/events")
    public String eventsStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = resolveDateRange(startDate, endDate);
        addDateRangeToModel(model, dates[0], dates[1]);

        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "events");
        model.addAttribute("pageTitle", "Event Statistics");
        model.addAttribute("data", statisticsService.getEventStatistics(dates[0], dates[1]));
        return "admin/events";
    }

    /**
     * Retention statistics page
     */
    @GetMapping("/statistics/retention")
    public String retentionStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request,
            Model model) {
        LocalDate[] dates = resolveDateRange(startDate, endDate);
        addDateRangeToModel(model, dates[0], dates[1]);

        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "retention");
        model.addAttribute("pageTitle", "Retention Analysis");
        model.addAttribute("data", statisticsService.getRetentionStatistics(dates[0], dates[1]));
        return "admin/retention";
    }

    /**
     * Marketing target page
     */
    @GetMapping("/statistics/marketing")
    public String marketingTargets(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "marketing");
        model.addAttribute("pageTitle", "Marketing Targets");
        model.addAttribute("data", statisticsService.getMarketingTargets());
        return "admin/marketing";
    }

    // ==================== Email Page ====================

    /**
     * Email send page
     */
    @GetMapping("/email")
    public String emailPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentPage", "email");
        model.addAttribute("pageTitle", "Send Email");
        return "admin/email";
    }

    // ==================== Helper Methods ====================

    /**
     * Resolve date range with defaults (1 year ago ~ today)
     */
    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return new LocalDate[]{startDate, endDate};
    }

    /**
     * Add date range attributes to model
     */
    private void addDateRangeToModel(Model model, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        model.addAttribute("startDate", startDate.format(formatter));
        model.addAttribute("endDate", endDate.format(formatter));
    }
}
