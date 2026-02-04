package side.onetime.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import side.onetime.dto.admin.email.response.UserSearchResult;
import side.onetime.dto.admin.statistics.response.*;
import side.onetime.repository.StatisticsRepository;
import side.onetime.repository.UserRepository;
import side.onetime.repository.custom.StatisticsRepositoryCustom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StatisticsRepository statisticsRepository;

    @Mock
    private StatisticsRepositoryCustom statisticsRepositoryCustom;

    @InjectMocks
    private StatisticsService statisticsService;

    // ==================== User Search ====================

    @Test
    @DisplayName("유저를 검색한다")
    public void searchUsers() {
        // given
        List<Object[]> userRows = new ArrayList<>();
        userRows.add(new Object[]{1L, "홍길동", "hong@example.com", "길동이", "KAKAO"});

        when(statisticsRepository.searchUsersByNameOrEmail("홍길동", 20)).thenReturn(userRows);

        // when
        List<UserSearchResult> result = statisticsService.searchUsers("홍길동", 20);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("홍길동");
        assertThat(result.get(0).email()).isEqualTo("hong@example.com");
    }

    // ==================== Funnel Analysis ====================

    @Test
    @DisplayName("전환 퍼널을 분석한다")
    public void getFunnelAnalysis() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        when(statisticsRepository.countSignups(startDateTime, endDateTime)).thenReturn(1000L);
        when(statisticsRepository.countUsersWithFirstEvent(startDateTime, endDateTime)).thenReturn(300L);
        when(statisticsRepository.countUsersWithParticipants(startDateTime, endDateTime)).thenReturn(150L);
        when(statisticsRepository.countUsersWithSecondEvent(startDateTime, endDateTime)).thenReturn(50L);

        // when
        FunnelAnalysisResponse result = statisticsService.getFunnelAnalysis(startDate, endDate);

        // then
        assertThat(result.steps()).hasSize(4);
        assertThat(result.totalSignups()).isEqualTo(1000);

        // 가입 단계
        FunnelAnalysisResponse.FunnelStep signupStep = result.steps().get(0);
        assertThat(signupStep.name()).isEqualTo("signup");
        assertThat(signupStep.count()).isEqualTo(1000);
        assertThat(signupStep.rate()).isEqualTo(100.0);
        assertThat(signupStep.dropoffRate()).isEqualTo(0.0);

        // 첫 이벤트 단계
        FunnelAnalysisResponse.FunnelStep firstEventStep = result.steps().get(1);
        assertThat(firstEventStep.name()).isEqualTo("first_event");
        assertThat(firstEventStep.count()).isEqualTo(300);
        assertThat(firstEventStep.rate()).isEqualTo(30.0);
        assertThat(firstEventStep.dropoffRate()).isEqualTo(70.0);

        // 참여자 받은 단계
        FunnelAnalysisResponse.FunnelStep participantStep = result.steps().get(2);
        assertThat(participantStep.name()).isEqualTo("received_participant");
        assertThat(participantStep.count()).isEqualTo(150);
        assertThat(participantStep.rate()).isEqualTo(15.0);

        // 두번째 이벤트 단계
        FunnelAnalysisResponse.FunnelStep secondEventStep = result.steps().get(3);
        assertThat(secondEventStep.name()).isEqualTo("second_event");
        assertThat(secondEventStep.count()).isEqualTo(50);
        assertThat(secondEventStep.rate()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("가입자가 없으면 전환율이 0이다")
    public void getFunnelAnalysisWithNoSignups() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        when(statisticsRepository.countSignups(startDateTime, endDateTime)).thenReturn(0L);
        when(statisticsRepository.countUsersWithFirstEvent(startDateTime, endDateTime)).thenReturn(0L);
        when(statisticsRepository.countUsersWithParticipants(startDateTime, endDateTime)).thenReturn(0L);
        when(statisticsRepository.countUsersWithSecondEvent(startDateTime, endDateTime)).thenReturn(0L);

        // when
        FunnelAnalysisResponse result = statisticsService.getFunnelAnalysis(startDate, endDate);

        // then
        assertThat(result.totalSignups()).isEqualTo(0);
        assertThat(result.steps().get(1).rate()).isEqualTo(0.0);
    }

    // ==================== Cohort Retention ====================

    @Test
    @DisplayName("코호트 리텐션을 분석한다")
    public void getCohortRetention() {
        // given
        List<Object[]> cohortSizes = new ArrayList<>();
        cohortSizes.add(new Object[]{"2025-01", 100L});
        cohortSizes.add(new Object[]{"2025-02", 80L});

        List<Object[]> cohortActivity = new ArrayList<>();
        cohortActivity.add(new Object[]{"2025-01", "2025-01", 100L});
        cohortActivity.add(new Object[]{"2025-01", "2025-02", 45L});
        cohortActivity.add(new Object[]{"2025-02", "2025-02", 80L});
        cohortActivity.add(new Object[]{"2025-02", "2025-03", 35L});

        when(statisticsRepository.findCohortSizes(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(cohortSizes);
        when(statisticsRepository.findCohortMonthlyActivity(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(cohortActivity);

        // when
        CohortRetentionResponse result = statisticsService.getCohortRetention(12);

        // then
        assertThat(result.cohorts()).hasSize(2);
        assertThat(result.periods()).hasSize(12);

        // 첫 번째 코호트 (2025-01)
        CohortRetentionResponse.CohortRow firstCohort = result.cohorts().get(0);
        assertThat(firstCohort.month()).isEqualTo("2025-01");
        assertThat(firstCohort.size()).isEqualTo(100);
        assertThat(firstCohort.retention().get(0)).isEqualTo(100.0); // M0
        assertThat(firstCohort.retention().get(1)).isEqualTo(45.0);  // M1
    }

    // ==================== TTV Distribution ====================

    @Test
    @DisplayName("TTV 분포를 분석한다")
    public void getTtvDistribution() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        when(statisticsRepository.countSignups(startDateTime, endDateTime)).thenReturn(1000L);
        when(statisticsRepository.countUsersWithAnyEvent(startDateTime, endDateTime)).thenReturn(850L);

        // TTV 분포: 당일(0일) 500명, 1-3일 200명, 4-7일 100명, 8-14일 50명
        List<Integer> ttvDays = new ArrayList<>();
        for (int i = 0; i < 500; i++) ttvDays.add(0);   // 당일
        for (int i = 0; i < 200; i++) ttvDays.add(2);   // 1-3일
        for (int i = 0; i < 100; i++) ttvDays.add(5);   // 4-7일
        for (int i = 0; i < 50; i++) ttvDays.add(10);   // 8-14일

        when(statisticsRepository.findTtvDistribution(startDateTime, endDateTime)).thenReturn(ttvDays);

        // when
        TtvDistributionResponse result = statisticsService.getTtvDistribution(startDate, endDate);

        // then
        assertThat(result.totalUsers()).isEqualTo(1000);
        assertThat(result.usersWithEvent()).isEqualTo(850);
        assertThat(result.activationRate()).isEqualTo(85.0);
        assertThat(result.distribution()).hasSize(6); // 6개 버킷

        // 당일 버킷 확인
        TtvDistributionResponse.TtvBucket sameDayBucket = result.distribution().get(0);
        assertThat(sameDayBucket.label()).isEqualTo("당일");
        assertThat(sameDayBucket.count()).isEqualTo(500);
    }

    // ==================== Heatmap ====================

    @Test
    @DisplayName("이벤트 생성 히트맵을 조회한다")
    public void getTimeWeekdayHeatmap() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // hour, dayOfWeek(MySQL: 1=Sun, 2=Mon, ...), count
        List<Object[]> heatmapData = new ArrayList<>();
        heatmapData.add(new Object[]{10, 2, 50L});  // 10시, 월요일, 50개
        heatmapData.add(new Object[]{14, 3, 30L});  // 14시, 화요일, 30개
        heatmapData.add(new Object[]{20, 6, 100L}); // 20시, 금요일, 100개

        when(statisticsRepository.findEventCreationHeatmap(startDateTime, endDateTime)).thenReturn(heatmapData);

        // when
        TimeWeekdayHeatmapResponse result = statisticsService.getTimeWeekdayHeatmap(startDate, endDate);

        // then
        assertThat(result.hours()).hasSize(24);
        assertThat(result.weekdays()).hasSize(7);
        assertThat(result.data()).hasSize(24);
        assertThat(result.maxValue()).isEqualTo(100);
        assertThat(result.totalEvents()).isEqualTo(180); // 50 + 30 + 100

        // 10시 월요일 (dayIndex = 0)
        assertThat(result.data().get(10).get(0)).isEqualTo(50);
        // 20시 금요일 (dayIndex = 4)
        assertThat(result.data().get(20).get(4)).isEqualTo(100);
    }

    // ==================== Stickiness ====================

    @Test
    @DisplayName("WAU/MAU 점착도를 조회한다")
    public void getStickiness() {
        // given
        when(statisticsRepository.countMau(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(600L);
        when(statisticsRepository.countWau(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(150L);

        List<Object[]> monthlyWauData = new ArrayList<>();
        monthlyWauData.add(new Object[]{"2025-01", 100.0});
        monthlyWauData.add(new Object[]{"2025-02", 120.0});

        List<Object[]> monthlyMauData = new ArrayList<>();
        monthlyMauData.add(new Object[]{"2025-01", 500L});
        monthlyMauData.add(new Object[]{"2025-02", 550L});

        when(statisticsRepository.findMonthlyAvgWau(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlyWauData);
        when(statisticsRepository.findMonthlyMau(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlyMauData);

        // when
        StickinessResponse result = statisticsService.getStickiness(12);

        // then
        assertThat(result.currentWau()).isEqualTo(150);
        assertThat(result.currentMau()).isEqualTo(600);
        assertThat(result.currentStickiness()).isEqualTo(25.0); // 150/600 * 100

        assertThat(result.trend()).hasSize(2);
        assertThat(result.trend().get(0).month()).isEqualTo("2025-01");
        assertThat(result.trend().get(0).stickiness()).isEqualTo(20.0); // 100/500 * 100
    }

    // ==================== Marketing Targets ====================

    @Test
    @DisplayName("마케팅 타겟을 조회한다")
    public void getMarketingTargets() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        when(statisticsRepository.countMarketingAgreedUsersByDateRange(startDateTime, endDateTime)).thenReturn(500L);
        when(statisticsRepository.countDormantUsersByDateRange(startDateTime, endDateTime)).thenReturn(100L);
        when(statisticsRepository.countNoEventUsersByDateRange(startDateTime, endDateTime)).thenReturn(200L);
        when(statisticsRepository.countOneTimeUsersByDateRange(startDateTime, endDateTime)).thenReturn(150L);
        when(statisticsRepository.countVipUsersByDateRange(startDateTime, endDateTime)).thenReturn(50L);
        when(statisticsRepository.countZeroParticipantEventsByDateRange(startDateTime, endDateTime)).thenReturn(80L);

        // when
        MarketingTargetsResponse result = statisticsService.getMarketingTargets(startDate, endDate);

        // then
        assertThat(result.marketingAgreedUsers()).isEqualTo(500);
        assertThat(result.dormantUsers()).isEqualTo(100);
        assertThat(result.noEventUsers()).isEqualTo(200);
        assertThat(result.oneTimeUsers()).isEqualTo(150);
        assertThat(result.vipUsers()).isEqualTo(50);
        assertThat(result.zeroParticipantEvents()).isEqualTo(80);
    }

    // ==================== Marketing Target Details ====================

    @Test
    @DisplayName("마케팅 동의 유저 상세를 조회한다")
    public void getMarketingAgreedUsers() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Object[]> mockRows = new ArrayList<>();
        // 14 columns: userId, email, name, nickname, provider, providerId,
        // servicePolicyAgreement, privacyPolicyAgreement, marketingPolicyAgreement,
        // sleepStartTime, sleepEndTime, language, createdDate, updatedDate
        java.sql.Timestamp createdDate = java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 1, 15, 10, 0));
        mockRows.add(new Object[]{
                1L, "hong@example.com", "홍길동", "길동이", "KAKAO", "kakao_123",
                true, true, true, "23:00", "07:00", "KOR", createdDate, createdDate
        });

        when(statisticsRepositoryCustom.findMarketingAgreedUserDetailsWithSortAndSearch(
                anyString(), any(), eq(startDateTime), eq(endDateTime))).thenReturn(mockRows);

        // when
        MarketingTargetDetailResponse result = statisticsService.getMarketingAgreedUsers(
                "created_date_desc", null, startDate, endDate);

        // then
        assertThat(result.type()).isEqualTo("agreed");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.users()).hasSize(1);
        assertThat(result.users().get(0).name()).isEqualTo("홍길동");
        assertThat(result.users().get(0).email()).isEqualTo("hong@example.com");
    }

    @Test
    @DisplayName("휴면 유저 상세를 조회한다")
    public void getDormantUsers() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Object[]> mockRows = new ArrayList<>();
        // 16 columns for dormant: 14 basic + lastLogin + daysInactive
        java.sql.Timestamp createdDate = java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 10, 0));
        java.sql.Timestamp lastLogin = java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 1, 15, 10, 0));
        mockRows.add(new Object[]{
                1L, "kim@example.com", "김철수", "철수", "GOOGLE", "google_123",
                true, true, true, "23:00", "07:00", "KOR", createdDate, createdDate,
                lastLogin, 45  // lastLogin, daysInactive
        });

        when(statisticsRepositoryCustom.findDormantUserDetailsWithSortAndSearch(
                eq(30), anyString(), any(), eq(startDateTime), eq(endDateTime))).thenReturn(mockRows);

        // when
        MarketingTargetDetailResponse result = statisticsService.getDormantUsers(
                30, "created_date_desc", null, startDate, endDate);

        // then
        assertThat(result.type()).isEqualTo("dormant");
        assertThat(result.users().get(0).name()).isEqualTo("김철수");
    }

    // ==================== User Detail ====================

    @Test
    @DisplayName("유저 상세 정보를 조회한다")
    public void getUserDetail() {
        // given
        Long userId = 1L;

        List<Object[]> basicInfo = new ArrayList<>();
        basicInfo.add(new Object[]{
                "홍길동", "길동이", "hong@example.com", "KAKAO", "KOR",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 10, 0)),
                true
        });

        List<Object[]> tokenInfo = new ArrayList<>();
        tokenInfo.add(new Object[]{
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                "192.168.1.1",
                1L
        });

        when(statisticsRepository.findUserBasicInfo(userId)).thenReturn(basicInfo);
        when(statisticsRepository.findUserTokenInfo(userId)).thenReturn(tokenInfo);
        when(statisticsRepository.countUserCreatedEvents(userId)).thenReturn(5L);
        when(statisticsRepository.countUserParticipatedEvents(userId)).thenReturn(10L);

        // when
        UserDetailResponse result = statisticsService.getUserDetail(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.nickname()).isEqualTo("길동이");
        assertThat(result.email()).isEqualTo("hong@example.com");
        assertThat(result.provider()).isEqualTo("KAKAO");
        assertThat(result.marketingAgreement()).isTrue();
        assertThat(result.createdEventCount()).isEqualTo(5);
        assertThat(result.participatedEventCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("존재하지 않는 유저 상세 정보 조회 시 null을 반환한다")
    public void getUserDetailNotFound() {
        // given
        Long userId = 999L;
        when(statisticsRepository.findUserBasicInfo(userId)).thenReturn(new ArrayList<>());

        // when
        UserDetailResponse result = statisticsService.getUserDetail(userId);

        // then
        assertThat(result).isNull();
    }
}
