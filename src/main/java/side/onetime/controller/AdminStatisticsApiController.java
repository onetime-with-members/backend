package side.onetime.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.email.response.UserSearchResult;
import side.onetime.dto.admin.response.GetAllDashboardEventsResponse;
import side.onetime.dto.admin.response.GetAllDashboardUsersResponse;
import side.onetime.dto.admin.statistics.response.CohortRetentionResponse;
import side.onetime.dto.admin.statistics.response.FunnelAnalysisResponse;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.dto.admin.statistics.response.StickinessResponse;
import side.onetime.dto.admin.statistics.response.TimeWeekdayHeatmapResponse;
import side.onetime.dto.admin.statistics.response.TtvDistributionResponse;
import side.onetime.dto.admin.statistics.response.UserDetailResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.AdminService;
import side.onetime.service.StatisticsService;
import side.onetime.util.DateUtil;

/**
 * Admin Statistics API Controller
 * 마케팅 타겟 상세 목록 조회용 API
 *
 * 공통 파라미터:
 * - sort: created_date_desc(기본), created_date_asc, name_asc, name_desc
 * - search: 이름, 이메일, 닉네임 검색
 */
@Hidden
@RestController
@RequestMapping("/admin/api/statistics")
@RequiredArgsConstructor
public class AdminStatisticsApiController {

    private final StatisticsService statisticsService;
    private final AdminService adminService;

    // ==================== Events / Users 목록 ====================

    /**
     * 이벤트 목록 조회 (페이지네이션, 정렬, 검색, 기간 필터)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기 (기본 10)
     * @param keyword 정렬 기준 (created_date, end_time, participant_count)
     * @param sorting 정렬 방향 (asc, desc)
     * @param search 검색어 (제목)
     * @param startDate 시작일 (생성일 기준)
     * @param endDate 종료일 (생성일 기준)
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<GetAllDashboardEventsResponse>> getAllEvents(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_date") String keyword,
            @RequestParam(defaultValue = "desc") String sorting,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Pageable pageable = PageRequest.of(page - 1, size);
        GetAllDashboardEventsResponse response = adminService.getAllDashboardEvents(pageable, keyword, sorting, search, startDate, endDate);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DASHBOARD_EVENTS, response);
    }

    /**
     * 유저 목록 조회 (페이지네이션, 정렬, 검색, 기간 필터)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기 (기본 10)
     * @param keyword 정렬 기준 (name, email, created_date, participation_count)
     * @param sorting 정렬 방향 (asc, desc)
     * @param search 검색어 (이름, 이메일, 닉네임)
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일 (가입일 기준)
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<GetAllDashboardUsersResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_date") String keyword,
            @RequestParam(defaultValue = "desc") String sorting,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Pageable pageable = PageRequest.of(page - 1, size);
        GetAllDashboardUsersResponse response = adminService.getAllDashboardUsers(pageable, keyword, sorting, search, startDate, endDate);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DASHBOARD_USERS, response);
    }

    // ==================== User Search (Email) ====================

    /**
     * 유저 검색 (이름/이메일/닉네임)
     * 이메일 발송 시 특정 유저 선택용
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<UserSearchResult>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        List<UserSearchResult> results = statisticsService.searchUsers(query, limit);
        return ApiResponse.onSuccess(SuccessStatus._OK, results);
    }

    /**
     * 유저 상세 정보 조회
     */
    @GetMapping("/users/{userId}/detail")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(@PathVariable Long userId) {
        UserDetailResponse response = statisticsService.getUserDetail(userId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // ==================== Funnel Analysis ====================

    /**
     * 전환 퍼널 분석
     * 가입 -> 첫 이벤트 생성 -> 참여자 1명+ 받음 -> 2번째 이벤트 생성
     *
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일
     */
    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<FunnelAnalysisResponse>> getFunnelAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);
        FunnelAnalysisResponse response = statisticsService.getFunnelAnalysis(dates[0], dates[1]);
        return ApiResponse.onSuccess(SuccessStatus._GET_FUNNEL_ANALYSIS, response);
    }

    // ==================== Cohort Retention ====================

    /**
     * 코호트 리텐션 분석
     * 가입월 기준 코호트별 M0~M11 리텐션율 반환
     *
     * @param months 분석할 코호트 수 (기본 12개월)
     */
    @GetMapping("/cohort")
    public ResponseEntity<ApiResponse<CohortRetentionResponse>> getCohortRetention(
            @RequestParam(defaultValue = "12") int months) {
        CohortRetentionResponse response = statisticsService.getCohortRetention(months);
        return ApiResponse.onSuccess(SuccessStatus._GET_COHORT_RETENTION, response);
    }

    // ==================== TTV (Time to Value) ====================

    /**
     * TTV 분포 분석
     * 가입 후 첫 이벤트 생성까지 걸린 시간 분포
     *
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일
     */
    @GetMapping("/ttv")
    public ResponseEntity<ApiResponse<TtvDistributionResponse>> getTtvDistribution(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);
        TtvDistributionResponse response = statisticsService.getTtvDistribution(dates[0], dates[1]);
        return ApiResponse.onSuccess(SuccessStatus._GET_TTV_DISTRIBUTION, response);
    }

    // ==================== Time × Weekday Heatmap ====================

    /**
     * 이벤트 생성 시간대 × 요일 히트맵
     *
     * @param startDate 시작일
     * @param endDate 종료일
     */
    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<TimeWeekdayHeatmapResponse>> getTimeWeekdayHeatmap(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] dates = DateUtil.resolveDateRange(startDate, endDate);
        TimeWeekdayHeatmapResponse response = statisticsService.getTimeWeekdayHeatmap(dates[0], dates[1]);
        return ApiResponse.onSuccess(SuccessStatus._GET_TIME_WEEKDAY_HEATMAP, response);
    }

    // ==================== WAU/MAU Stickiness ====================

    /**
     * WAU/MAU 점착도 분석
     *
     * @param months 분석할 개월 수 (기본 12)
     */
    @GetMapping("/stickiness")
    public ResponseEntity<ApiResponse<StickinessResponse>> getStickiness(
            @RequestParam(defaultValue = "12") int months) {
        StickinessResponse response = statisticsService.getStickiness(months);
        return ApiResponse.onSuccess(SuccessStatus._GET_STICKINESS, response);
    }

    // ==================== Marketing Targets ====================

    /**
     * 마케팅 동의 유저 목록
     */
    @GetMapping("/marketing/agreed")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getMarketingAgreedUsers(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getMarketingAgreedUsers(limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }

    /**
     * 휴면 유저 목록 (마케팅 동의자만)
     */
    @GetMapping("/marketing/dormant")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getDormantUsers(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getDormantUsers(days, limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }

    /**
     * 이벤트 미생성 유저 목록
     */
    @GetMapping("/marketing/no-event")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getNoEventUsers(
            @RequestParam(defaultValue = "7") int daysAfterSignup,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getNoEventUsers(daysAfterSignup, limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }

    /**
     * 1회성 유저 목록
     */
    @GetMapping("/marketing/one-time")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getOneTimeUsers(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getOneTimeUsers(limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }

    /**
     * VIP 유저 목록
     */
    @GetMapping("/marketing/vip")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getVipUsers(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getVipUsers(limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }

    /**
     * 참여자 0명 이벤트 목록
     */
    @GetMapping("/marketing/zero-participant")
    public ResponseEntity<ApiResponse<MarketingTargetDetailResponse>> getZeroParticipantEvents(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "created_date_desc") String sort,
            @RequestParam(required = false) String search) {
        MarketingTargetDetailResponse response = statisticsService.getZeroParticipantEvents(limit, sort, search);
        return ApiResponse.onSuccess(SuccessStatus._GET_MARKETING_TARGET_DETAIL, response);
    }
}
