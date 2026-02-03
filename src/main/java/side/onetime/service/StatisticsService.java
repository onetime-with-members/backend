package side.onetime.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.dto.admin.email.response.UserSearchResult;
import side.onetime.dto.admin.statistics.response.CohortRetentionResponse;
import side.onetime.dto.admin.statistics.response.DashboardChartsResponse;
import side.onetime.dto.admin.statistics.response.DashboardSummaryResponse;
import side.onetime.dto.admin.statistics.response.EventStatisticsResponse;
import side.onetime.dto.admin.statistics.response.FunnelAnalysisResponse;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.dto.admin.statistics.response.MarketingTargetsResponse;
import side.onetime.dto.admin.statistics.response.RetentionStatisticsResponse;
import side.onetime.dto.admin.statistics.response.StickinessResponse;
import side.onetime.dto.admin.statistics.response.TimeWeekdayHeatmapResponse;
import side.onetime.dto.admin.statistics.response.TtvDistributionResponse;
import side.onetime.dto.admin.statistics.response.UserDetailResponse;
import side.onetime.dto.admin.statistics.response.UserStatisticsResponse;
import side.onetime.repository.StatisticsRepository;
import side.onetime.repository.UserRepository;
import side.onetime.repository.custom.StatisticsRepositoryCustom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final StatisticsRepository statisticsRepository;
    private final StatisticsRepositoryCustom statisticsRepositoryCustom;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // ==================== Utility Methods ====================

    /**
     * 날짜 범위를 DateTime 범위로 변환 (startDate 00:00:00 ~ endDate+1 00:00:00)
     */
    private record DateTimeRange(LocalDateTime start, LocalDateTime end) {
        static DateTimeRange of(LocalDate startDate, LocalDate endDate) {
            return new DateTimeRange(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay()
            );
        }
    }

    /**
     * Object[] 결과에서 Long 값 추출 (null-safe)
     */
    private long extractLong(Object[] row, int index) {
        return row != null && row[index] != null ? ((Number) row[index]).longValue() : 0L;
    }

    /**
     * Object[] 결과에서 Double 값 추출 (null-safe)
     */
    private double extractDouble(Object[] row, int index) {
        return row != null && row[index] != null ? ((Number) row[index]).doubleValue() : 0.0;
    }

    /**
     * List<Object[]> 결과에서 첫 번째 row 추출 (null-safe)
     */
    private Object[] getFirstRow(List<Object[]> results) {
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    /**
     * 비율 계산 (소수점 1자리 반올림)
     */
    private double calculateRate(long numerator, long denominator) {
        return denominator > 0 ? Math.round((double) numerator / denominator * 1000.0) / 10.0 : 0.0;
    }

    /**
     * Long null을 0으로 변환
     */
    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    // ==================== Public Service Methods ====================

    /**
     * 유저 검색 (이름/이메일/닉네임)
     * 이메일 발송 시 특정 유저 선택용
     */
    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(String query, int limit) {
        return statisticsRepository.searchUsersByNameOrEmail(query, limit)
                .stream()
                .map(UserSearchResult::from)
                .toList();
    }

    /**
     * Get dashboard summary statistics
     * refresh_token 기반 MAU 적용 (설계 문서 12.3 쿼리)
     * Native Query로 최적화됨
     * 이전 기간 대비 증감률 포함
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // 현재 기간 통계 조회
        SummaryData current = getSummaryDataForRange(range);

        // 이전 기간 계산 (동일 길이의 이전 기간)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysBetween);
        DateTimeRange prevRange = DateTimeRange.of(prevStartDate, prevEndDate);

        // 이전 기간 통계 조회
        SummaryData previous = getSummaryDataForRange(prevRange);

        // 비교 데이터 생성
        DashboardSummaryResponse.ComparisonData comparison = DashboardSummaryResponse.ComparisonData.of(
                current.totalUsers, previous.totalUsers,
                current.totalEvents, previous.totalEvents,
                current.mau, previous.mau,
                current.avgParticipants, previous.avgParticipants,
                current.dormantRate, previous.dormantRate,
                current.marketingTargetUsers, previous.marketingTargetUsers
        );

        return DashboardSummaryResponse.of(
                current.totalUsers, current.activeUsers, current.totalEvents, current.mau,
                Math.round(current.avgParticipants * 100.0) / 100.0,
                Math.round(current.dormantRate * 100.0) / 100.0,
                current.marketingTargetUsers,
                comparison
        );
    }

    /**
     * 기간별 요약 통계 데이터 조회 (내부 헬퍼)
     */
    private SummaryData getSummaryDataForRange(DateTimeRange range) {
        // Native Query로 유저 통계 조회 (totalUsers, marketingAgreed)
        Object[] userStats = getFirstRow(statisticsRepository.getUserStatsByDateRange(range.start(), range.end()));
        long totalUsers = extractLong(userStats, 0);
        long marketingTargetUsers = extractLong(userStats, 1);

        // Native Query로 이벤트 통계 조회
        Object[] eventStats = getFirstRow(statisticsRepository.getEventStatsByDateRange(range.start(), range.end()));
        long totalEvents = extractLong(eventStats, 0);

        // Active users - refresh_token 기반 (기간 내 로그인)
        List<Object[]> dauData = statisticsRepository.findDailyActiveUsers(range.start(), range.end());
        long activeUsers = dauData.stream()
                .mapToLong(row -> extractLong(row, 1))
                .max()
                .orElse(0L);

        // MAU - refresh_token 기반 (기간 내)
        List<Object[]> mauData = statisticsRepository.findMonthlyActiveUsers(range.start(), range.end());
        long mau = mauData.stream()
                .mapToLong(row -> extractLong(row, 1))
                .sum();

        // Average participants per event - Native Query 사용
        Object[] avgData = getFirstRow(statisticsRepository.getAvgParticipantsData(range.start(), range.end()));
        double avgParticipants = 0;
        if (avgData != null) {
            long eventCount = extractLong(avgData, 0);
            long epCount = extractLong(avgData, 1);
            long memberCount = extractLong(avgData, 2);
            if (eventCount > 0) {
                avgParticipants = (double) (epCount + memberCount) / eventCount;
            }
        }

        // Dormant rate (기간 내 가입 유저 중 60일+ 미접속 비율)
        Object[] dormantData = getFirstRow(statisticsRepository.countDormantRateByDateRange(range.start(), range.end()));
        long dormantUsers = extractLong(dormantData, 0);
        long totalUsersInRange = extractLong(dormantData, 1);
        double dormantRate = totalUsersInRange > 0 ? (double) dormantUsers / totalUsersInRange * 100 : 0;

        return new SummaryData(totalUsers, activeUsers, totalEvents, mau, avgParticipants, dormantRate, marketingTargetUsers);
    }

    /**
     * 요약 통계 데이터 홀더
     */
    private record SummaryData(
            long totalUsers,
            long activeUsers,
            long totalEvents,
            long mau,
            double avgParticipants,
            double dormantRate,
            long marketingTargetUsers
    ) {}

    /**
     * Get dashboard chart data
     * Native Query로 최적화됨
     * 이전 기간 비교 데이터 포함
     */
    @Transactional(readOnly = true)
    public DashboardChartsResponse getDashboardCharts(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // 이전 기간 계산
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysBetween);
        DateTimeRange prevRange = DateTimeRange.of(prevStartDate, prevEndDate);

        // Monthly signups with comparison - Native Query 사용
        DashboardChartsResponse.ChartDataWithComparison monthlySignups =
                getMonthlySignupsWithComparisonFromNativeQuery(range, prevRange, startDate, endDate, prevStartDate, prevEndDate);

        // Provider distribution - Native Query 사용
        DashboardChartsResponse.ChartData providers = getProviderDistributionFromNativeQuery(range.start(), range.end());

        // Weekday distribution - Native Query 사용
        DashboardChartsResponse.ChartData weekdayDistribution = getWeekdayDistributionFromNativeQuery(range.start(), range.end());

        // Top keywords - Native Query 사용
        List<DashboardChartsResponse.KeywordItem> topKeywords = getTopKeywordsFromNativeQuery(range.start(), range.end());

        return DashboardChartsResponse.of(monthlySignups, providers, weekdayDistribution, topKeywords);
    }

    /**
     * Get user statistics
     * Native Query로 최적화됨
     */
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // Native Query로 유저 통계 조회
        Object[] userStats = getFirstRow(statisticsRepository.getUserStatsByDateRange(range.start(), range.end()));
        long totalUsers = extractLong(userStats, 0);
        long marketingAgreed = extractLong(userStats, 1);

        // 활성 유저 = 선택 기간 내 고유 로그인 유저 수 (refresh_token.last_used_at 기준)
        Long activeUsersResult = statisticsRepository.countMau(range.start(), range.end());
        long activeUsers = activeUsersResult != null ? activeUsersResult : 0L;

        long deletedUsers = 0; // Soft deleted users are not returned

        // Marketing agreement rate
        double marketingAgreementRate = totalUsers > 0 ? (double) marketingAgreed / totalUsers * 100 : 0;

        // Provider distribution - Native Query
        Map<String, Long> providerDistribution = statisticsRepository.getProviderDistribution(range.start(), range.end()).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Language distribution - Native Query
        Map<String, Long> languageDistribution = statisticsRepository.getLanguageDistribution(range.start(), range.end()).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Monthly signups - Native Query (기존 메서드 활용)
        List<UserStatisticsResponse.MonthlyData> monthlySignups = statisticsRepository.findMonthlySignups(range.start(), range.end()).stream()
                .map(row -> new UserStatisticsResponse.MonthlyData((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return UserStatisticsResponse.of(
                totalUsers, activeUsers, deletedUsers,
                Math.round(marketingAgreementRate * 100.0) / 100.0,
                providerDistribution, languageDistribution, monthlySignups
        );
    }

    /**
     * Get event statistics
     * Native Query로 최적화됨
     */
    @Transactional(readOnly = true)
    public EventStatisticsResponse getEventStatistics(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // Native Query로 이벤트 통계 조회
        Object[] eventStats = getFirstRow(statisticsRepository.getEventStatsByDateRange(range.start(), range.end()));
        long totalEvents = extractLong(eventStats, 0);
        long activeEvents = totalEvents; // All returned events are active

        // Average participants - Native Query
        Object[] avgData = getFirstRow(statisticsRepository.getAvgParticipantsData(range.start(), range.end()));
        double avgParticipants = 0;
        if (avgData != null) {
            long eventCount = extractLong(avgData, 0);
            long epCount = extractLong(avgData, 1);
            long memberCount = extractLong(avgData, 2);
            if (eventCount > 0) {
                avgParticipants = (double) (epCount + memberCount) / eventCount;
            }
        }

        // Category distribution - Native Query
        Map<String, Long> categoryDistribution = statisticsRepository.getCategoryDistribution(range.start(), range.end()).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> extractLong(row, 1)
                ));

        // Weekday distribution - Native Query (DAYOFWEEK: 1=SUN, 2=MON, ..., 7=SAT)
        String[] dayNames = {"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"};
        Map<String, Long> weekdayDistribution = statisticsRepository.getEventWeekdayDistribution(range.start(), range.end()).stream()
                .collect(Collectors.toMap(
                        row -> dayNames[((Number) row[0]).intValue() - 1],
                        row -> extractLong(row, 1)
                ));

        // Monthly events - Native Query (이벤트용 쿼리 필요 - 기존 signups 쿼리 패턴 활용)
        List<EventStatisticsResponse.MonthlyData> monthlyEvents = getMonthlyEventsFromNativeQuery(range.start(), range.end());

        // Top keywords - Native Query
        List<EventStatisticsResponse.KeywordData> topKeywords = extractTopKeywordsFromNativeQuery(range.start(), range.end());

        return EventStatisticsResponse.of(
                totalEvents, activeEvents,
                Math.round(avgParticipants * 100.0) / 100.0,
                categoryDistribution, weekdayDistribution, monthlyEvents, topKeywords
        );
    }

    /**
     * Get retention statistics
     * refresh_token 기반 MAU 및 휴면 유저 분석 (설계 문서 12.3 쿼리 적용)
     * 모든 지표가 선택한 기간 내 가입 유저 기준으로 계산됨
     */
    @Transactional(readOnly = true)
    public RetentionStatisticsResponse getRetentionStatistics(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // Monthly MAU (refresh_token 기반 - 로그인 사용자만)
        List<Object[]> mauData = statisticsRepository.findMonthlyActiveUsers(range.start(), range.end());
        List<RetentionStatisticsResponse.MonthlyMau> monthlyMau = mauData.stream()
                .map(row -> new RetentionStatisticsResponse.MonthlyMau(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .toList();

        // Dormant users by period (refresh_token.last_used_at 기준)
        // 7일+, 30일+, 90일+ 각각 해당 기간 이상 미접속 유저 전체
        // 기간 내 가입 유저 기준
        List<Object[]> dormantDistribution = statisticsRepository.findDormantUserDistribution(range.start(), range.end());
        Map<String, Long> dormantMap = dormantDistribution.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        long dormant7Only = dormantMap.getOrDefault("7+", 0L);   // 7-29일
        long dormant30Only = dormantMap.getOrDefault("30+", 0L); // 30-89일
        long dormant90Only = dormantMap.getOrDefault("90+", 0L); // 90일+

        // 누적 계산: N일+ = N일 이상 미접속 전체
        long dormant7 = dormant7Only + dormant30Only + dormant90Only;  // 7일 이상 전체
        long dormant30 = dormant30Only + dormant90Only;                 // 30일 이상 전체
        long dormant90 = dormant90Only;                                 // 90일 이상만

        // Returning user rate (이벤트 2개+ 참여 유저) - 기간 내 가입 유저 기준
        Long returningUsers = statisticsRepository.countReturningUsers(range.start(), range.end());
        Long usersWithEvents = statisticsRepository.countUsersWithEvents(range.start(), range.end());
        double returningUserRate = usersWithEvents != null && usersWithEvents > 0 ?
                (double) returningUsers / usersWithEvents * 100 : 0;

        // Average days to first event - 기간 내 가입 유저 기준
        Double avgDaysToFirstEvent = statisticsRepository.findAverageDaysToFirstEvent(range.start(), range.end());
        if (avgDaysToFirstEvent == null) {
            avgDaysToFirstEvent = 0.0;
        }

        return RetentionStatisticsResponse.of(
                monthlyMau,
                dormant7,   // 7일+ 미접속 전체
                dormant30,  // 30일+ 미접속 전체
                dormant90,  // 90일+ 미접속만
                Math.round(returningUserRate * 100.0) / 100.0,
                Math.round(avgDaysToFirstEvent * 100.0) / 100.0
        );
    }

    /**
     * Get funnel analysis statistics
     * 전환 퍼널 분석: 가입 -> 첫 이벤트 생성 -> 참여자 받음 -> 2번째 이벤트 생성
     *
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일
     * @return 퍼널 분석 데이터
     */
    @Transactional(readOnly = true)
    public FunnelAnalysisResponse getFunnelAnalysis(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        // Step counts (null-safe)
        long signups = nullToZero(statisticsRepository.countSignups(range.start(), range.end()));
        long firstEvent = nullToZero(statisticsRepository.countUsersWithFirstEvent(range.start(), range.end()));
        long withParticipants = nullToZero(statisticsRepository.countUsersWithParticipants(range.start(), range.end()));
        long secondEvent = nullToZero(statisticsRepository.countUsersWithSecondEvent(range.start(), range.end()));

        // Calculate rates (from signup)
        double signupRate = 100.0;
        double firstEventRate = signups > 0 ? (double) firstEvent / signups * 100 : 0;
        double participantsRate = signups > 0 ? (double) withParticipants / signups * 100 : 0;
        double secondEventRate = signups > 0 ? (double) secondEvent / signups * 100 : 0;

        // Calculate drop-off rates
        double dropoff1 = signupRate - firstEventRate;
        double dropoff2 = firstEventRate - participantsRate;
        double dropoff3 = participantsRate - secondEventRate;

        List<FunnelAnalysisResponse.FunnelStep> steps = List.of(
                FunnelAnalysisResponse.FunnelStep.of("signup", "가입", signups,
                        roundRate(signupRate), 0),
                FunnelAnalysisResponse.FunnelStep.of("first_event", "첫 이벤트 생성", firstEvent,
                        roundRate(firstEventRate), roundRate(dropoff1)),
                FunnelAnalysisResponse.FunnelStep.of("received_participant", "참여자 1명+ 받음", withParticipants,
                        roundRate(participantsRate), roundRate(dropoff2)),
                FunnelAnalysisResponse.FunnelStep.of("second_event", "2번째 이벤트 생성", secondEvent,
                        roundRate(secondEventRate), roundRate(dropoff3))
        );

        return FunnelAnalysisResponse.of(steps, signups);
    }

    private double roundRate(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * Get cohort retention analysis
     * 가입월 기준 코호트별 M0~M11 리텐션율 계산
     * refresh_token.last_used_at 기준 활성 판단
     *
     * @param months 분석할 코호트 수 (기본 12개월)
     * @return 코호트 리텐션 데이터
     */
    @Transactional(readOnly = true)
    public CohortRetentionResponse getCohortRetention(int months) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months);

        // 1. 코호트별 가입자 수 조회
        Map<String, Integer> cohortSizes = statisticsRepository.findCohortSizes(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // 2. 코호트별 월별 활성 유저 조회
        Map<String, Map<String, Integer>> activityMatrix = statisticsRepository
                .findCohortMonthlyActivity(startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row[0],  // cohort_month
                        Collectors.toMap(
                                row -> (String) row[1],  // active_month
                                row -> ((Number) row[2]).intValue(),
                                (v1, v2) -> v1  // 중복 키 처리
                        )
                ));

        // 3. 리텐션율 계산
        List<CohortRetentionResponse.CohortRow> cohorts = cohortSizes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildCohortRow(
                        entry.getKey(),
                        entry.getValue(),
                        activityMatrix.getOrDefault(entry.getKey(), Map.of()),
                        months
                ))
                .toList();

        return CohortRetentionResponse.of(cohorts, months);
    }

    /**
     * 코호트 행 데이터 빌드
     */
    private CohortRetentionResponse.CohortRow buildCohortRow(
            String cohortMonth, int size, Map<String, Integer> activity, int maxPeriods) {
        YearMonth cohort = YearMonth.parse(cohortMonth);
        List<Double> retention = new ArrayList<>();

        for (int i = 0; i < maxPeriods; i++) {
            String targetMonth = cohort.plusMonths(i).toString();
            int activeCount = activity.getOrDefault(targetMonth, 0);
            double rate = size > 0 ? Math.round((double) activeCount / size * 1000.0) / 10.0 : 0;
            retention.add(rate);
        }

        return CohortRetentionResponse.CohortRow.of(cohortMonth, size, retention);
    }

    /**
     * Get marketing targets (전체 기간)
     */
    @Transactional(readOnly = true)
    public MarketingTargetsResponse getMarketingTargets() {
        return getMarketingTargets(LocalDate.of(2000, 1, 1), LocalDate.now().plusDays(1));
    }

    /**
     * Get marketing targets (날짜 범위 필터)
     * 해당 기간 내 가입한 유저 / 생성된 이벤트 기준
     */
    @Transactional(readOnly = true)
    public MarketingTargetsResponse getMarketingTargets(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 기간 내 가입한 유저 중 마케팅 동의 유저
        long marketingAgreed = nullToZero(statisticsRepository.countMarketingAgreedUsersByDateRange(startDateTime, endDateTime));

        // 기간 내 가입한 유저 중 휴면 유저 (30일+ 미접속)
        long dormantUsers = nullToZero(statisticsRepository.countDormantUsersByDateRange(startDateTime, endDateTime));

        // 기간 내 가입한 유저 중 이벤트 미생성 (가입 7일 경과)
        long noEventUsers = nullToZero(statisticsRepository.countNoEventUsersByDateRange(startDateTime, endDateTime));

        // 기간 내 가입한 유저 중 일회성 유저
        long oneTimeUsers = nullToZero(statisticsRepository.countOneTimeUsersByDateRange(startDateTime, endDateTime));

        // 기간 내 가입한 유저 중 VIP 유저
        long vipUsers = nullToZero(statisticsRepository.countVipUsersByDateRange(startDateTime, endDateTime));

        // 기간 내 생성된 이벤트 중 참여자 0명
        long zeroParticipantEvents = nullToZero(statisticsRepository.countZeroParticipantEventsByDateRange(startDateTime, endDateTime));

        return MarketingTargetsResponse.of(
                marketingAgreed, dormantUsers, noEventUsers,
                oneTimeUsers, vipUsers, zeroParticipantEvents
        );
    }

    // ==================== Marketing Target Details ====================

    /**
     * 마케팅 동의 유저 상세 목록
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getMarketingAgreedUsers(int limit, String sort, String search) {
        // Native Query로 마케팅 동의 유저 수 조회
        List<Object[]> userStatsList = statisticsRepository.getUserStatsByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now().plusDays(1)
        );
        Object[] userStats = userStatsList.isEmpty() ? null : userStatsList.get(0);
        long totalCount = userStats != null && userStats[1] != null ? ((Number) userStats[1]).longValue() : 0;

        List<Object[]> rows = statisticsRepositoryCustom.findMarketingAgreedUserDetailsWithSortAndSearch(limit, sort, search);
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "agreed"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("agreed", totalCount, users);
    }

    /**
     * 휴면 유저 상세 목록 (마케팅 동의자만)
     * 정렬, 검색, 기간 필터 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getDormantUsers(int days, int limit, String sort, String search,
                                                          LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        List<Object[]> countRows = statisticsRepository.countDormantUsersWithMarketing(days, range.start(), range.end());
        long totalCount = countRows.size();

        List<Object[]> rows = statisticsRepositoryCustom.findDormantUserDetailsWithSortAndSearch(
                days, limit, sort, search, range.start(), range.end());
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "dormant"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("dormant", totalCount, users);
    }

    /**
     * 이벤트 미생성 유저 상세 목록
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getNoEventUsers(int daysAfterSignup, int limit, String sort, String search) {
        long totalCount = nullToZero(statisticsRepository.countNoEventUsers(daysAfterSignup));

        List<Object[]> rows = statisticsRepositoryCustom.findNoEventUserDetailsWithSortAndSearch(daysAfterSignup, limit, sort, search);
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "noEvent"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("noEvent", totalCount, users);
    }

    /**
     * 1회성 유저 상세 목록
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getOneTimeUsers(int limit, String sort, String search) {
        long totalCount = nullToZero(statisticsRepository.countOneTimeUsers());

        List<Object[]> rows = statisticsRepositoryCustom.findOneTimeUserDetailsWithSortAndSearch(limit, sort, search);
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "oneTime"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("oneTime", totalCount, users);
    }

    /**
     * VIP 유저 상세 목록
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getVipUsers(int limit, String sort, String search) {
        long totalCount = nullToZero(statisticsRepository.countVipUsers());

        List<Object[]> rows = statisticsRepositoryCustom.findVipUserDetailsWithSortAndSearch(limit, sort, search);
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "vip"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("vip", totalCount, users);
    }

    /**
     * 참여자 0명 이벤트 상세 목록
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getZeroParticipantEvents(int limit, String sort, String search) {
        long totalCount = nullToZero(statisticsRepository.countZeroParticipantEvents());

        List<Object[]> rows = statisticsRepositoryCustom.findZeroParticipantEventDetailsWithSortAndSearch(limit, sort, search);
        List<MarketingTargetDetailResponse.EventDetail> events = rows.stream()
                .map(MarketingTargetDetailResponse.EventDetail::fromRow)
                .toList();

        return MarketingTargetDetailResponse.ofEvents("zeroParticipant", totalCount, events);
    }

    /**
     * 복귀 유저 상세 목록 (이벤트 2회+ 참여)
     * 기간 필터 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getReturningUsers(LocalDate startDate, LocalDate endDate, int limit) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        long totalCount = nullToZero(statisticsRepository.countReturningUsers(range.start(), range.end()));

        List<Object[]> rows = statisticsRepository.findReturningUserDetails(range.start(), range.end(), limit);
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "returning"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("returning", totalCount, users);
    }

    /**
     * 휴면 유저 상세 목록 (리텐션용 - 모든 유저)
     * 기간 필터 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getDormantUsersForRetention(int days, int limit,
                                                                      LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        long totalCount = nullToZero(statisticsRepository.countDormantUsersForRetention(days, range.start(), range.end()));

        List<Object[]> rows = statisticsRepository.findDormantUserDetailsForRetention(days, limit, range.start(), range.end());
        List<MarketingTargetDetailResponse.UserDetail> users = rows.stream()
                .map(row -> MarketingTargetDetailResponse.UserDetail.fromRow(row, "dormant"))
                .toList();

        return MarketingTargetDetailResponse.ofUsers("dormant", totalCount, users);
    }

    // ==================== Native Query 기반 헬퍼 메서드 ====================

    private DashboardChartsResponse.ChartData getMonthlySignupsFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDate startDate, LocalDate endDate) {
        Map<String, Long> monthlyData = new TreeMap<>();

        // Initialize months in range
        long monthsBetween = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
        for (int i = 0; i < monthsBetween; i++) {
            String month = startDate.plusMonths(i).format(MONTH_FORMATTER);
            monthlyData.put(month, 0L);
        }

        // Native Query로 월별 가입자 수 조회
        statisticsRepository.findMonthlySignups(startDateTime, endDateTime).forEach(row -> {
            String month = (String) row[0];
            long count = ((Number) row[1]).longValue();
            monthlyData.put(month, count);
        });

        return DashboardChartsResponse.ChartData.of(
                new ArrayList<>(monthlyData.keySet()),
                new ArrayList<>(monthlyData.values())
        );
    }

    /**
     * 이전 기간 비교 데이터를 포함한 월별 가입자 조회
     */
    private DashboardChartsResponse.ChartDataWithComparison getMonthlySignupsWithComparisonFromNativeQuery(
            DateTimeRange range, DateTimeRange prevRange,
            LocalDate startDate, LocalDate endDate,
            LocalDate prevStartDate, LocalDate prevEndDate) {

        // 현재 기간 데이터
        Map<String, Long> currentData = new TreeMap<>();
        long monthsBetween = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
        for (int i = 0; i < monthsBetween; i++) {
            String month = startDate.plusMonths(i).format(MONTH_FORMATTER);
            currentData.put(month, 0L);
        }
        statisticsRepository.findMonthlySignups(range.start(), range.end()).forEach(row -> {
            String month = (String) row[0];
            long count = ((Number) row[1]).longValue();
            currentData.put(month, count);
        });

        // 이전 기간 데이터
        Map<String, Long> previousData = new TreeMap<>();
        long prevMonthsBetween = ChronoUnit.MONTHS.between(prevStartDate.withDayOfMonth(1), prevEndDate.withDayOfMonth(1)) + 1;
        for (int i = 0; i < prevMonthsBetween; i++) {
            String month = prevStartDate.plusMonths(i).format(MONTH_FORMATTER);
            previousData.put(month, 0L);
        }
        statisticsRepository.findMonthlySignups(prevRange.start(), prevRange.end()).forEach(row -> {
            String month = (String) row[0];
            long count = ((Number) row[1]).longValue();
            previousData.put(month, count);
        });

        return DashboardChartsResponse.ChartDataWithComparison.of(
                new ArrayList<>(currentData.keySet()),
                new ArrayList<>(currentData.values()),
                new ArrayList<>(previousData.values()),
                new ArrayList<>(previousData.keySet())
        );
    }

    private DashboardChartsResponse.ChartData getProviderDistributionFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Long> distribution = statisticsRepository.getProviderDistribution(startDateTime, endDateTime).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        return DashboardChartsResponse.ChartData.of(
                new ArrayList<>(distribution.keySet()),
                new ArrayList<>(distribution.values())
        );
    }

    private DashboardChartsResponse.ChartData getWeekdayDistributionFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        // DAYOFWEEK: 1=SUN, 2=MON, 3=TUE, 4=WED, 5=THU, 6=FRI, 7=SAT
        String[] weekdays = {"월", "화", "수", "목", "금", "토", "일"};
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String day : weekdays) {
            distribution.put(day, 0L);
        }

        // MySQL DAYOFWEEK을 요일명으로 변환
        Map<Integer, String> dayMap = Map.of(
                1, "일", 2, "월", 3, "화", 4, "수",
                5, "목", 6, "금", 7, "토"
        );

        statisticsRepository.getEventWeekdayDistribution(startDateTime, endDateTime).forEach(row -> {
            int dayOfWeek = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            String dayName = dayMap.get(dayOfWeek);
            if (dayName != null) {
                distribution.put(dayName, count);
            }
        });

        return DashboardChartsResponse.ChartData.of(
                new ArrayList<>(distribution.keySet()),
                new ArrayList<>(distribution.values())
        );
    }

    private List<DashboardChartsResponse.KeywordItem> getTopKeywordsFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Object[]> keywordDataList = statisticsRepository.getKeywordCounts(startDateTime, endDateTime);

        if (keywordDataList == null || keywordDataList.isEmpty()) {
            return List.of();
        }

        Object[] keywordData = keywordDataList.get(0);

        // 키워드 30개 (쿼리 순서와 동일)
        String[] keywords = getKeywordArray();
        Map<String, Long> keywordCounts = new HashMap<>();

        for (int i = 0; i < keywords.length && i < keywordData.length; i++) {
            long count = keywordData[i] != null ? ((Number) keywordData[i]).longValue() : 0;
            if (count > 0) {
                keywordCounts.put(keywords[i], count);
            }
        }

        long total = keywordCounts.values().stream().mapToLong(Long::longValue).sum();

        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> DashboardChartsResponse.KeywordItem.of(
                        e.getKey(),
                        e.getValue(),
                        total > 0 ? Math.round((double) e.getValue() / total * 10000.0) / 100.0 : 0
                ))
                .toList();
    }

    private List<EventStatisticsResponse.MonthlyData> getMonthlyEventsFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return statisticsRepository.findMonthlyEvents(startDateTime, endDateTime).stream()
                .map(row -> new EventStatisticsResponse.MonthlyData(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private List<EventStatisticsResponse.KeywordData> extractTopKeywordsFromNativeQuery(
            LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Object[]> keywordDataList = statisticsRepository.getKeywordCounts(startDateTime, endDateTime);

        if (keywordDataList == null || keywordDataList.isEmpty()) {
            return List.of();
        }

        Object[] keywordData = keywordDataList.get(0);

        String[] keywords = getKeywordArray();
        Map<String, Long> keywordCounts = new HashMap<>();

        for (int i = 0; i < keywords.length && i < keywordData.length; i++) {
            long count = keywordData[i] != null ? ((Number) keywordData[i]).longValue() : 0;
            if (count > 0) {
                keywordCounts.put(keywords[i], count);
            }
        }

        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> new EventStatisticsResponse.KeywordData(e.getKey(), e.getValue()))
                .toList();
    }

    private String[] getKeywordArray() {
        return new String[]{
            "회의", "스터디", "밥", "술", "MT", "면접", "프로젝트", "동아리", "모임", "여행",
            "점심", "저녁", "식사", "커피", "미팅", "팀", "워크샵", "세미나", "강의", "수업",
            "운동", "헬스", "축구", "농구", "야구", "테니스", "골프", "등산", "캠핑", "파티"
        };
    }

    /**
     * Get TTV (Time to Value) Distribution
     * 가입 후 첫 이벤트 생성까지 걸린 시간 분포 분석
     *
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일
     * @return TTV 분포 데이터
     */
    @Transactional(readOnly = true)
    public TtvDistributionResponse getTtvDistribution(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);

        long totalUsers = nullToZero(statisticsRepository.countSignups(range.start(), range.end()));
        long usersWithEvent = nullToZero(statisticsRepository.countUsersWithAnyEvent(range.start(), range.end()));

        List<Integer> ttvDays = statisticsRepository.findTtvDistribution(range.start(), range.end());

        double averageDays = 0;
        double medianDays = 0;
        if (!ttvDays.isEmpty()) {
            averageDays = Math.round(ttvDays.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0;
            medianDays = ttvDays.get(ttvDays.size() / 2);
        }

        double activationRate = calculateRate(usersWithEvent, totalUsers);
        List<TtvDistributionResponse.TtvBucket> distribution = buildTtvBuckets(ttvDays, usersWithEvent);

        return TtvDistributionResponse.of(averageDays, medianDays, totalUsers, usersWithEvent, activationRate, distribution);
    }

    /**
     * TTV 분포 버킷 생성
     */
    private List<TtvDistributionResponse.TtvBucket> buildTtvBuckets(List<Integer> ttvDays, long total) {
        // 버킷 정의: 당일, 1-3일, 4-7일, 8-14일, 15-30일, 31일+
        int[][] bucketRanges = {
                {0, 0},
                {1, 3},
                {4, 7},
                {8, 14},
                {15, 30},
                {31, Integer.MAX_VALUE}
        };
        String[] labels = {"당일", "1-3일", "4-7일", "8-14일", "15-30일", "31일+"};

        List<TtvDistributionResponse.TtvBucket> buckets = new ArrayList<>();

        for (int i = 0; i < bucketRanges.length; i++) {
            int min = bucketRanges[i][0];
            int max = bucketRanges[i][1];
            String label = labels[i];

            long count = ttvDays.stream()
                    .filter(d -> d >= min && d <= max)
                    .count();

            double percentage = total > 0 ? Math.round((double) count / total * 1000.0) / 10.0 : 0;
            buckets.add(TtvDistributionResponse.TtvBucket.of(label, min, max == Integer.MAX_VALUE ? -1 : max, count, percentage));
        }

        return buckets;
    }

    /**
     * Get Time × Weekday Heatmap
     * 이벤트 생성 시간대(0-23시) × 요일(월-일) 히트맵
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 히트맵 데이터
     */
    @Transactional(readOnly = true)
    public TimeWeekdayHeatmapResponse getTimeWeekdayHeatmap(LocalDate startDate, LocalDate endDate) {
        DateTimeRange range = DateTimeRange.of(startDate, endDate);
        List<Object[]> rawData = statisticsRepository.findEventCreationHeatmap(range.start(), range.end());

        // Initialize 24x7 matrix (hours x weekdays)
        // MySQL DAYOFWEEK: 1=Sun, 2=Mon, ..., 7=Sat
        // We want: 0=Mon, 1=Tue, ..., 6=Sun
        List<List<Long>> data = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            List<Long> row = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                row.add(0L);
            }
            data.add(row);
        }

        long maxValue = 0;
        long totalEvents = 0;

        for (Object[] row : rawData) {
            int hour = ((Number) row[0]).intValue();
            int mysqlDayOfWeek = ((Number) row[1]).intValue(); // 1=Sun, 2=Mon, ...
            long count = ((Number) row[2]).longValue();

            // Convert MySQL dayOfWeek to our index (Mon=0, Tue=1, ..., Sun=6)
            int dayIndex = (mysqlDayOfWeek + 5) % 7;

            data.get(hour).set(dayIndex, count);
            maxValue = Math.max(maxValue, count);
            totalEvents += count;
        }

        List<String> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(String.format("%02d", h));
        }

        List<String> weekdays = List.of("월", "화", "수", "목", "금", "토", "일");

        return TimeWeekdayHeatmapResponse.of(hours, weekdays, data, maxValue, totalEvents);
    }

    /**
     * Get WAU/MAU Stickiness
     * 서비스 점착도 = WAU / MAU × 100
     * - WAU: 최근 7일 활성 유저
     * - MAU: 최근 30일 활성 유저
     *
     * @param months 분석할 개월 수
     * @return 점착도 데이터
     */
    @Transactional(readOnly = true)
    public StickinessResponse getStickiness(int months) {
        LocalDate now = LocalDate.now();
        LocalDateTime endDate = now.plusDays(1).atStartOfDay();

        // MAU: 최근 30일 활성 유저
        LocalDateTime mauStart = now.minusDays(29).atStartOfDay();
        long currentMau = nullToZero(statisticsRepository.countMau(mauStart, endDate));

        // WAU: 최근 7일 활성 유저
        LocalDateTime wauStart = now.minusDays(6).atStartOfDay();
        long currentWau = nullToZero(statisticsRepository.countWau(wauStart, endDate));

        // Current stickiness
        double currentStickiness = currentMau > 0 ? Math.round((double) currentWau / currentMau * 1000.0) / 10.0 : 0;

        // Build monthly trend
        LocalDateTime trendStart = now.minusMonths(months).withDayOfMonth(1).atStartOfDay();
        List<Object[]> monthlyWauData = statisticsRepository.findMonthlyAvgWau(trendStart, endDate);
        List<Object[]> monthlyMauData = statisticsRepository.findMonthlyMau(trendStart, endDate);

        // Build MAU map
        Map<String, Long> mauMap = new HashMap<>();
        for (Object[] row : monthlyMauData) {
            String month = (String) row[0];
            long mau = ((Number) row[1]).longValue();
            mauMap.put(month, mau);
        }

        // Build trend
        List<StickinessResponse.MonthlyStickiness> trend = new ArrayList<>();
        for (Object[] row : monthlyWauData) {
            String month = (String) row[0];
            long avgWau = Math.round(((Number) row[1]).doubleValue());
            long mau = mauMap.getOrDefault(month, 0L);
            double stickiness = mau > 0 ? Math.round((double) avgWau / mau * 1000.0) / 10.0 : 0;
            trend.add(StickinessResponse.MonthlyStickiness.of(month, avgWau, mau, stickiness));
        }

        return StickinessResponse.of(currentStickiness, currentWau, currentMau, trend);
    }

    // ==================== 유저 상세 정보 ====================

    /**
     * 유저 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        // 기본 정보
        List<Object[]> basicInfoList = statisticsRepository.findUserBasicInfo(userId);
        if (basicInfoList.isEmpty()) {
            return null;
        }
        Object[] basicInfo = basicInfoList.get(0);

        String name = (String) basicInfo[0];
        String nickname = (String) basicInfo[1];
        String email = (String) basicInfo[2];
        String provider = (String) basicInfo[3];
        String language = basicInfo[4] != null ? basicInfo[4].toString() : null;
        LocalDateTime createdDate = basicInfo[5] != null ? ((java.sql.Timestamp) basicInfo[5]).toLocalDateTime() : null;
        boolean marketingAgreement = false;
        if (basicInfo[6] != null) {
            if (basicInfo[6] instanceof Boolean) {
                marketingAgreement = (Boolean) basicInfo[6];
            } else if (basicInfo[6] instanceof Number) {
                marketingAgreement = ((Number) basicInfo[6]).intValue() == 1;
            }
        }

        // 토큰 정보
        List<Object[]> tokenInfoList = statisticsRepository.findUserTokenInfo(userId);
        Object[] tokenInfo = tokenInfoList.isEmpty() ? null : tokenInfoList.get(0);

        LocalDateTime lastUsedAt = null;
        String lastUsedIp = null;
        int activeSessionCount = 0;

        if (tokenInfo != null) {
            lastUsedAt = tokenInfo[0] != null ? ((java.sql.Timestamp) tokenInfo[0]).toLocalDateTime() : null;
            lastUsedIp = (String) tokenInfo[1];
            activeSessionCount = tokenInfo[2] != null ? ((Number) tokenInfo[2]).intValue() : 0;
        }

        int inactiveDays = lastUsedAt != null
                ? (int) ChronoUnit.DAYS.between(lastUsedAt.toLocalDate(), LocalDate.now())
                : -1;

        // 이벤트 수
        Long createdCount = statisticsRepository.countUserCreatedEvents(userId);
        Long participatedCount = statisticsRepository.countUserParticipatedEvents(userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return UserDetailResponse.of(
                userId,
                name,
                nickname,
                email,
                provider,
                language,
                createdDate != null ? createdDate.format(formatter) : null,
                marketingAgreement,
                lastUsedAt != null ? lastUsedAt.format(formatter) : null,
                lastUsedIp,
                activeSessionCount,
                inactiveDays,
                createdCount != null ? createdCount.intValue() : 0,
                participatedCount != null ? participatedCount.intValue() : 0
        );
    }
}
