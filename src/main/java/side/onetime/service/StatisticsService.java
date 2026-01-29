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
import side.onetime.dto.admin.statistics.response.CohortRetentionResponse;
import side.onetime.dto.admin.statistics.response.FunnelAnalysisResponse;
import side.onetime.dto.admin.statistics.response.StickinessResponse;
import side.onetime.dto.admin.statistics.response.TimeWeekdayHeatmapResponse;
import side.onetime.dto.admin.statistics.response.TtvDistributionResponse;
import side.onetime.dto.admin.statistics.response.DashboardChartsResponse;
import side.onetime.dto.admin.statistics.response.DashboardSummaryResponse;
import side.onetime.dto.admin.statistics.response.EventStatisticsResponse;
import side.onetime.dto.admin.email.response.UserSearchResult;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.dto.admin.statistics.response.MarketingTargetsResponse;
import side.onetime.dto.admin.statistics.response.RetentionStatisticsResponse;
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
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Native Query로 유저 통계 조회 (totalUsers, marketingAgreed)
        List<Object[]> userStatsList = statisticsRepository.getUserStatsByDateRange(startDateTime, endDateTime);
        Object[] userStats = userStatsList.isEmpty() ? null : userStatsList.get(0);
        long totalUsers = userStats != null && userStats[0] != null ? ((Number) userStats[0]).longValue() : 0;
        long marketingTargetUsers = userStats != null && userStats[1] != null ? ((Number) userStats[1]).longValue() : 0;

        // Native Query로 이벤트 통계 조회
        List<Object[]> eventStatsList = statisticsRepository.getEventStatsByDateRange(startDateTime, endDateTime);
        Object[] eventStats = eventStatsList.isEmpty() ? null : eventStatsList.get(0);
        long totalEvents = eventStats != null && eventStats[0] != null ? ((Number) eventStats[0]).longValue() : 0;

        // Active users - refresh_token 기반 (기간 내 로그인)
        List<Object[]> dauData = statisticsRepository.findDailyActiveUsers(startDateTime, endDateTime);
        long activeUsers = dauData.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .max()
                .orElse(0L);

        // MAU - refresh_token 기반 (기간 내)
        List<Object[]> mauData = statisticsRepository.findMonthlyActiveUsers(startDateTime, endDateTime);
        long mau = mauData.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        // Average participants per event - Native Query 사용
        List<Object[]> avgDataList = statisticsRepository.getAvgParticipantsData(startDateTime, endDateTime);
        Object[] avgData = avgDataList.isEmpty() ? null : avgDataList.get(0);
        double avgParticipants = 0;
        if (avgData != null && avgData[0] != null) {
            long eventCount = ((Number) avgData[0]).longValue();
            long epCount = avgData[1] != null ? ((Number) avgData[1]).longValue() : 0;
            long memberCount = avgData[2] != null ? ((Number) avgData[2]).longValue() : 0;
            if (eventCount > 0) {
                avgParticipants = (double) (epCount + memberCount) / eventCount;
            }
        }

        // Dormant rate (refresh_token 기반 60일+ 미접속)
        List<Object[]> dormantDistribution = statisticsRepository.findDormantUserDistribution();
        long dormantUsers = dormantDistribution.stream()
                .filter(row -> "60-89".equals(row[0]) || "90+".equals(row[0]))
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
        long totalUserCount = userRepository.count();
        double dormantRate = totalUserCount > 0 ? (double) dormantUsers / totalUserCount * 100 : 0;

        return DashboardSummaryResponse.of(
                totalUsers, activeUsers, totalEvents, mau,
                Math.round(avgParticipants * 100.0) / 100.0,
                Math.round(dormantRate * 100.0) / 100.0,
                marketingTargetUsers
        );
    }

    /**
     * Get dashboard chart data
     * Native Query로 최적화됨
     */
    @Transactional(readOnly = true)
    public DashboardChartsResponse getDashboardCharts(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Monthly signups - Native Query 사용
        DashboardChartsResponse.ChartData monthlySignups = getMonthlySignupsFromNativeQuery(startDateTime, endDateTime, startDate, endDate);

        // Provider distribution - Native Query 사용
        DashboardChartsResponse.ChartData providers = getProviderDistributionFromNativeQuery(startDateTime, endDateTime);

        // Weekday distribution - Native Query 사용
        DashboardChartsResponse.ChartData weekdayDistribution = getWeekdayDistributionFromNativeQuery(startDateTime, endDateTime);

        // Top keywords - Native Query 사용
        List<DashboardChartsResponse.KeywordItem> topKeywords = getTopKeywordsFromNativeQuery(startDateTime, endDateTime);

        return DashboardChartsResponse.of(monthlySignups, providers, weekdayDistribution, topKeywords);
    }

    /**
     * Get user statistics
     * Native Query로 최적화됨
     */
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Native Query로 유저 통계 조회
        List<Object[]> userStatsList = statisticsRepository.getUserStatsByDateRange(startDateTime, endDateTime);
        Object[] userStats = userStatsList.isEmpty() ? null : userStatsList.get(0);
        long totalUsers = userStats != null && userStats[0] != null ? ((Number) userStats[0]).longValue() : 0;
        long marketingAgreed = userStats != null && userStats[1] != null ? ((Number) userStats[1]).longValue() : 0;

        // DAU 기반 active users
        List<Object[]> dauData = statisticsRepository.findDailyActiveUsers(startDateTime, endDateTime);
        long activeUsers = dauData.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .max()
                .orElse(0L);

        long deletedUsers = 0; // Soft deleted users are not returned

        // Marketing agreement rate
        double marketingAgreementRate = totalUsers > 0 ? (double) marketingAgreed / totalUsers * 100 : 0;

        // Provider distribution - Native Query
        Map<String, Long> providerDistribution = statisticsRepository.getProviderDistribution(startDateTime, endDateTime).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Language distribution - Native Query
        Map<String, Long> languageDistribution = statisticsRepository.getLanguageDistribution(startDateTime, endDateTime).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Monthly signups - Native Query (기존 메서드 활용)
        List<UserStatisticsResponse.MonthlyData> monthlySignups = statisticsRepository.findMonthlySignups(startDateTime, endDateTime).stream()
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
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Native Query로 이벤트 통계 조회
        List<Object[]> eventStatsList = statisticsRepository.getEventStatsByDateRange(startDateTime, endDateTime);
        Object[] eventStats = eventStatsList.isEmpty() ? null : eventStatsList.get(0);
        long totalEvents = eventStats != null && eventStats[0] != null ? ((Number) eventStats[0]).longValue() : 0;
        long activeEvents = totalEvents; // All returned events are active

        // Average participants - Native Query
        List<Object[]> avgDataList = statisticsRepository.getAvgParticipantsData(startDateTime, endDateTime);
        Object[] avgData = avgDataList.isEmpty() ? null : avgDataList.get(0);
        double avgParticipants = 0;
        if (avgData != null && avgData[0] != null) {
            long eventCount = ((Number) avgData[0]).longValue();
            long epCount = avgData[1] != null ? ((Number) avgData[1]).longValue() : 0;
            long memberCount = avgData[2] != null ? ((Number) avgData[2]).longValue() : 0;
            if (eventCount > 0) {
                avgParticipants = (double) (epCount + memberCount) / eventCount;
            }
        }

        // Category distribution - Native Query
        Map<String, Long> categoryDistribution = statisticsRepository.getCategoryDistribution(startDateTime, endDateTime).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Weekday distribution - Native Query (DAYOFWEEK: 1=SUN, 2=MON, ..., 7=SAT)
        String[] dayNames = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        Map<String, Long> weekdayDistribution = statisticsRepository.getEventWeekdayDistribution(startDateTime, endDateTime).stream()
                .collect(Collectors.toMap(
                        row -> dayNames[((Number) row[0]).intValue() - 1],
                        row -> ((Number) row[1]).longValue()
                ));

        // Monthly events - Native Query (이벤트용 쿼리 필요 - 기존 signups 쿼리 패턴 활용)
        List<EventStatisticsResponse.MonthlyData> monthlyEvents = getMonthlyEventsFromNativeQuery(startDateTime, endDateTime);

        // Top keywords - Native Query
        List<EventStatisticsResponse.KeywordData> topKeywords = extractTopKeywordsFromNativeQuery(startDateTime, endDateTime);

        return EventStatisticsResponse.of(
                totalEvents, activeEvents,
                Math.round(avgParticipants * 100.0) / 100.0,
                categoryDistribution, weekdayDistribution, monthlyEvents, topKeywords
        );
    }

    /**
     * Get retention statistics
     * refresh_token 기반 MAU 및 휴면 유저 분석 (설계 문서 12.3 쿼리 적용)
     */
    @Transactional(readOnly = true)
    public RetentionStatisticsResponse getRetentionStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Monthly MAU (refresh_token 기반 - 로그인 사용자만)
        List<Object[]> mauData = statisticsRepository.findMonthlyActiveUsers(startDateTime, endDateTime);
        List<RetentionStatisticsResponse.MonthlyMau> monthlyMau = mauData.stream()
                .map(row -> new RetentionStatisticsResponse.MonthlyMau(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .toList();

        // Dormant users by period (refresh_token.last_used_at 기준)
        List<Object[]> dormantDistribution = statisticsRepository.findDormantUserDistribution();
        Map<String, Long> dormantMap = dormantDistribution.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        long dormant30 = dormantMap.getOrDefault("30-59", 0L);
        long dormant60 = dormantMap.getOrDefault("60-89", 0L);
        long dormant90 = dormantMap.getOrDefault("90+", 0L);

        // Returning user rate (이벤트 2개+ 참여 유저)
        Long returningUsers = statisticsRepository.countReturningUsers();
        Long usersWithEvents = statisticsRepository.countUsersWithEvents();
        double returningUserRate = usersWithEvents != null && usersWithEvents > 0 ?
                (double) returningUsers / usersWithEvents * 100 : 0;

        // Average days to first event
        Double avgDaysToFirstEvent = statisticsRepository.findAverageDaysToFirstEvent();
        if (avgDaysToFirstEvent == null) {
            avgDaysToFirstEvent = 0.0;
        }

        return RetentionStatisticsResponse.of(
                monthlyMau,
                dormant30,
                dormant60 + dormant30, // 60일+ = 60-89 + 30-59
                dormant90 + dormant60 + dormant30, // 90일+ = 모든 휴면
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
     * Get marketing targets
     * 네이티브 쿼리 사용 (설계 문서 12.3 쿼리 적용)
     * 모든 조회를 Native Query로 최적화
     */
    @Transactional(readOnly = true)
    public MarketingTargetsResponse getMarketingTargets() {
        // Marketing agreed users - Native Query로 카운트만 조회
        List<Object[]> userStatsList = statisticsRepository.getUserStatsByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0),  // 전체 기간
                LocalDateTime.now().plusDays(1)
        );
        Object[] userStats = userStatsList.isEmpty() ? null : userStatsList.get(0);
        long marketingAgreed = userStats != null && userStats[1] != null ? ((Number) userStats[1]).longValue() : 0;

        // Dormant users (30+ days) - refresh_token 기반
        List<Object[]> dormantDistribution = statisticsRepository.findDormantUserDistribution();
        long dormantUsers = dormantDistribution.stream()
                .filter(row -> !"active".equals(row[0]))
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        // Users with no events created (가입 7일 경과)
        Long noEventUsers = statisticsRepository.countNoEventUsers(7);
        if (noEventUsers == null) noEventUsers = 0L;

        // One-time users (created only 1 event)
        Long oneTimeUsers = statisticsRepository.countOneTimeUsers();
        if (oneTimeUsers == null) oneTimeUsers = 0L;

        // VIP users (5+ events)
        Long vipUsers = statisticsRepository.countVipUsers();
        if (vipUsers == null) vipUsers = 0L;

        // Events with zero participants
        Long zeroParticipantEvents = statisticsRepository.countZeroParticipantEvents();
        if (zeroParticipantEvents == null) zeroParticipantEvents = 0L;

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
     * 정렬, 검색 지원
     */
    @Transactional(readOnly = true)
    public MarketingTargetDetailResponse getDormantUsers(int days, int limit, String sort, String search) {
        List<Object[]> countRows = statisticsRepository.countDormantUsersWithMarketing(days);
        long totalCount = countRows.size();

        List<Object[]> rows = statisticsRepositoryCustom.findDormantUserDetailsWithSortAndSearch(days, limit, sort, search);
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
        Long totalCount = statisticsRepository.countNoEventUsers(daysAfterSignup);
        if (totalCount == null) totalCount = 0L;

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
        Long totalCount = statisticsRepository.countOneTimeUsers();
        if (totalCount == null) totalCount = 0L;

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
        Long totalCount = statisticsRepository.countVipUsers();
        if (totalCount == null) totalCount = 0L;

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
        Long totalCount = statisticsRepository.countZeroParticipantEvents();
        if (totalCount == null) totalCount = 0L;

        List<Object[]> rows = statisticsRepositoryCustom.findZeroParticipantEventDetailsWithSortAndSearch(limit, sort, search);
        List<MarketingTargetDetailResponse.EventDetail> events = rows.stream()
                .map(MarketingTargetDetailResponse.EventDetail::fromRow)
                .toList();

        return MarketingTargetDetailResponse.ofEvents("zeroParticipant", totalCount, events);
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
        String[] weekdays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String day : weekdays) {
            distribution.put(day, 0L);
        }

        // MySQL DAYOFWEEK을 요일명으로 변환
        Map<Integer, String> dayMap = Map.of(
                1, "SUN", 2, "MON", 3, "TUE", 4, "WED",
                5, "THU", 6, "FRI", 7, "SAT"
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

        Long totalUsers = statisticsRepository.countSignups(range.start(), range.end());
        Long usersWithEvent = statisticsRepository.countUsersWithAnyEvent(range.start(), range.end());
        totalUsers = totalUsers != null ? totalUsers : 0L;
        usersWithEvent = usersWithEvent != null ? usersWithEvent : 0L;

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

        List<String> weekdays = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        return TimeWeekdayHeatmapResponse.of(hours, weekdays, data, maxValue, totalEvents);
    }

    /**
     * Get WAU/MAU Stickiness
     * 서비스 점착도 = WAU / MAU × 100
     *
     * @param months 분석할 개월 수
     * @return 점착도 데이터
     */
    @Transactional(readOnly = true)
    public StickinessResponse getStickiness(int months) {
        LocalDate now = LocalDate.now();

        // Current month MAU
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.plusDays(1).atStartOfDay();
        Long currentMau = statisticsRepository.countMau(monthStart, monthEnd);
        currentMau = currentMau != null ? currentMau : 0L;

        // Current week WAU (last 7 days)
        LocalDateTime weekStart = now.minusDays(6).atStartOfDay();
        Long currentWau = statisticsRepository.countWau(weekStart, monthEnd);
        currentWau = currentWau != null ? currentWau : 0L;

        // Current stickiness
        double currentStickiness = currentMau > 0 ? Math.round((double) currentWau / currentMau * 1000.0) / 10.0 : 0;

        // Build monthly trend
        LocalDateTime trendStart = now.minusMonths(months).withDayOfMonth(1).atStartOfDay();
        List<Object[]> monthlyWauData = statisticsRepository.findMonthlyAvgWau(trendStart, monthEnd);
        List<Object[]> monthlyMauData = statisticsRepository.findMonthlyMau(trendStart, monthEnd);

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
}
