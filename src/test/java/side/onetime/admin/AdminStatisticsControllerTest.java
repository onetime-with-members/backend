package side.onetime.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import side.onetime.configuration.AdminControllerTestConfig;
import side.onetime.controller.AdminStatisticsController;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.Language;
import side.onetime.dto.admin.email.response.UserSearchResult;
import side.onetime.dto.admin.response.DashboardEvent;
import side.onetime.dto.admin.response.DashboardUser;
import side.onetime.dto.admin.response.GetAllDashboardEventsResponse;
import side.onetime.dto.admin.response.GetAllDashboardUsersResponse;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.admin.statistics.response.*;
import side.onetime.service.AdminService;
import side.onetime.service.StatisticsService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatisticsController.class)
public class AdminStatisticsControllerTest extends AdminControllerTestConfig {

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private AdminService adminService;

    // ==================== Events / Users 목록 ====================

    @Test
    @DisplayName("이벤트 목록을 조회한다")
    public void getAllEvents() throws Exception {
        // given
        List<DashboardEvent> events = List.of(
                new DashboardEvent(
                        1L, "uuid-1", "팀 회의", "09:00", "18:00",
                        Category.DATE, "홍길동", 5, "2025-03-01 10:00:00",
                        List.of("2025.03.15"), "2025.03.15", "09:00 - 18:00"
                )
        );
        PageInfo pageInfo = PageInfo.of(1, 10, 1, 1);
        GetAllDashboardEventsResponse response = GetAllDashboardEventsResponse.of(events, pageInfo);

        // when
        Mockito.when(adminService.getAllDashboardEvents(
                any(Pageable.class), anyString(), anyString(), any(), any(), any(), any(), any()
        )).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/events")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "created_date")
                        .param("sorting", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.dashboard_events").isArray())
                .andExpect(jsonPath("$.payload.dashboard_events[0].title").value("팀 회의"))
                .andExpect(jsonPath("$.payload.page_info.page").value(1));
    }

    @Test
    @DisplayName("유저 목록을 조회한다")
    public void getAllUsers() throws Exception {
        // given
        List<DashboardUser> users = List.of(
                new DashboardUser(
                        1L, "홍길동", "hong@example.com", "길동이",
                        "KAKAO", "kakao_123", true, true, false,
                        "23:00", "07:00", Language.KOR, 3, "2025-03-01 12:00:00"
                )
        );
        PageInfo pageInfo = PageInfo.of(1, 10, 1, 1);
        GetAllDashboardUsersResponse response = GetAllDashboardUsersResponse.of(users, pageInfo);

        // when
        Mockito.when(adminService.getAllDashboardUsers(
                any(Pageable.class), anyString(), anyString(), any(), any(), any()
        )).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/users")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.dashboard_users").isArray())
                .andExpect(jsonPath("$.payload.dashboard_users[0].name").value("홍길동"))
                .andExpect(jsonPath("$.payload.page_info.page").value(1));
    }

    // ==================== User Search ====================

    @Test
    @DisplayName("유저를 검색한다")
    public void searchUsers() throws Exception {
        // given
        List<UserSearchResult> results = List.of(
                new UserSearchResult(1L, "홍길동", "hong@example.com", "길동이", "KAKAO")
        );

        // when
        Mockito.when(statisticsService.searchUsers(anyString(), anyInt()))
                .thenReturn(results);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/users/search")
                        .param("query", "홍길동")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[0].name").value("홍길동"));
    }

    @Test
    @DisplayName("유저 상세 정보를 조회한다")
    public void getUserDetail() throws Exception {
        // given
        UserDetailResponse response = new UserDetailResponse(
                1L,
                "홍길동",
                "길동이",
                "hong@example.com",
                "KAKAO",
                "KOR",
                "2025-03-01",
                true,
                "2025-03-10 15:30:00",
                "192.168.1.1",
                1,
                0,
                5,
                10
        );

        // when
        Mockito.when(statisticsService.getUserDetail(anyLong()))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/users/{userId}/detail", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.name").value("홍길동"))
                .andExpect(jsonPath("$.payload.email").value("hong@example.com"));
    }

    // ==================== Funnel Analysis ====================

    @Test
    @DisplayName("전환 퍼널을 분석한다")
    public void getFunnelAnalysis() throws Exception {
        // given
        List<FunnelAnalysisResponse.FunnelStep> steps = List.of(
                FunnelAnalysisResponse.FunnelStep.of("signup", "가입", 1000, 100.0, 0.0),
                FunnelAnalysisResponse.FunnelStep.of("first_event", "첫 이벤트 생성", 300, 30.0, 70.0),
                FunnelAnalysisResponse.FunnelStep.of("participant", "참여자 확보", 150, 15.0, 50.0),
                FunnelAnalysisResponse.FunnelStep.of("recreate", "재생성", 50, 5.0, 66.7)
        );
        FunnelAnalysisResponse response = FunnelAnalysisResponse.of(steps, 1000);

        // when
        Mockito.when(statisticsService.getFunnelAnalysis(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/funnel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.steps").isArray())
                .andExpect(jsonPath("$.payload.steps[0].label").value("가입"))
                .andExpect(jsonPath("$.payload.steps[0].count").value(1000));
    }

    // ==================== Cohort Retention ====================

    @Test
    @DisplayName("코호트 리텐션을 분석한다")
    public void getCohortRetention() throws Exception {
        // given
        List<CohortRetentionResponse.CohortRow> cohorts = List.of(
                CohortRetentionResponse.CohortRow.of("2025-01", 100, List.of(100.0, 45.0, 30.0, 25.0))
        );
        CohortRetentionResponse response = CohortRetentionResponse.of(cohorts, 4);

        // when
        Mockito.when(statisticsService.getCohortRetention(anyInt()))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/cohort")
                        .param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.periods").isArray())
                .andExpect(jsonPath("$.payload.cohorts").isArray())
                .andExpect(jsonPath("$.payload.cohorts[0].month").value("2025-01"));
    }

    // ==================== TTV Distribution ====================

    @Test
    @DisplayName("TTV 분포를 분석한다")
    public void getTtvDistribution() throws Exception {
        // given
        List<TtvDistributionResponse.TtvBucket> distribution = List.of(
                TtvDistributionResponse.TtvBucket.of("당일", 0, 0, 500, 50.0),
                TtvDistributionResponse.TtvBucket.of("1-3일", 1, 3, 200, 20.0),
                TtvDistributionResponse.TtvBucket.of("4-7일", 4, 7, 150, 15.0)
        );
        TtvDistributionResponse response = TtvDistributionResponse.of(2.5, 1.0, 1000, 850, 85.0, distribution);

        // when
        Mockito.when(statisticsService.getTtvDistribution(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/ttv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.total_users").value(1000))
                .andExpect(jsonPath("$.payload.activation_rate").value(85.0))
                .andExpect(jsonPath("$.payload.distribution").isArray());
    }

    // ==================== Heatmap ====================

    @Test
    @DisplayName("이벤트 생성 히트맵을 조회한다")
    public void getTimeWeekdayHeatmap() throws Exception {
        // given
        List<String> hours = List.of("00", "01", "02");
        List<String> weekdays = List.of("월", "화", "수", "목", "금", "토", "일");
        List<List<Long>> data = List.of(
                List.of(0L, 1L, 2L, 3L, 4L, 5L, 6L),
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                List.of(2L, 3L, 4L, 5L, 6L, 7L, 8L)
        );
        TimeWeekdayHeatmapResponse response = TimeWeekdayHeatmapResponse.of(hours, weekdays, data, 8L, 100L);

        // when
        Mockito.when(statisticsService.getTimeWeekdayHeatmap(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.weekdays").isArray())
                .andExpect(jsonPath("$.payload.hours").isArray())
                .andExpect(jsonPath("$.payload.data").isArray())
                .andExpect(jsonPath("$.payload.max_value").value(8));
    }

    // ==================== Stickiness ====================

    @Test
    @DisplayName("WAU/MAU 점착도를 조회한다")
    public void getStickiness() throws Exception {
        // given
        List<StickinessResponse.MonthlyStickiness> trend = List.of(
                StickinessResponse.MonthlyStickiness.of("2025-01", 100, 500, 20.0),
                StickinessResponse.MonthlyStickiness.of("2025-02", 120, 550, 21.8)
        );
        StickinessResponse response = StickinessResponse.of(25.0, 150, 600, trend);

        // when
        Mockito.when(statisticsService.getStickiness(anyInt()))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/stickiness")
                        .param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.current_wau").value(150))
                .andExpect(jsonPath("$.payload.current_mau").value(600))
                .andExpect(jsonPath("$.payload.current_stickiness").value(25.0))
                .andExpect(jsonPath("$.payload.trend").isArray());
    }

    // ==================== Marketing Targets ====================

    @Test
    @DisplayName("마케팅 동의 유저를 조회한다")
    public void getMarketingAgreedUsers() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("홍길동")
                        .email("hong@example.com")
                        .nickname("길동이")
                        .provider("KAKAO")
                        .marketingPolicyAgreement(true)
                        .createdDate(java.time.LocalDateTime.of(2025, 3, 1, 10, 0))
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("agreed", 1, users);

        // when
        Mockito.when(statisticsService.getMarketingAgreedUsers(anyString(), any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/marketing/agreed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.totalCount").value(1))
                .andExpect(jsonPath("$.payload.users").isArray());
    }

    @Test
    @DisplayName("휴면 유저를 조회한다 (마케팅용)")
    public void getDormantUsers() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("김철수")
                        .email("kim@example.com")
                        .daysInactive(45)
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("dormant", 1, users);

        // when
        Mockito.when(statisticsService.getDormantUsers(anyInt(), anyString(), any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/marketing/dormant")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.totalCount").value(1));
    }

    @Test
    @DisplayName("복귀 유저를 조회한다")
    public void getReturningUsers() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("이영희")
                        .email("lee@example.com")
                        .eventCount(5)
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("returning", 1, users);

        // when
        Mockito.when(statisticsService.getReturningUsers(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/retention/returning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.totalCount").value(1));
    }

    @Test
    @DisplayName("휴면 유저를 조회한다 (리텐션용)")
    public void getDormantUsersForRetention() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("박민수")
                        .email("park@example.com")
                        .daysInactive(90)
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("dormant", 1, users);

        // when
        Mockito.when(statisticsService.getDormantUsersForRetention(anyInt(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/statistics/retention/dormant")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.totalCount").value(1));
    }
}
