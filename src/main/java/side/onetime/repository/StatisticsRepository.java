package side.onetime.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import side.onetime.domain.User;

/**
 * 통계 전용 Repository
 */
public interface StatisticsRepository extends JpaRepository<User, Long> {

    // ==================== DAU / MAU (refresh_token 기준) ====================

    /**
     * DAU (Daily Active Users) - refresh_token.last_used_at 기준 (회원만)
     * 날짜별 고유 사용자 수 조회
     * status 무관하게 last_used_at이 기간 내에 있으면 활성 사용자로 카운트
     */
    @Query(value = """
        SELECT DATE(last_used_at) AS date, COUNT(DISTINCT users_id) AS dau
        FROM refresh_token
        WHERE last_used_at >= :startDate AND last_used_at < :endDate
          AND user_type = 'USER'
        GROUP BY DATE(last_used_at)
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> findDailyActiveUsers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * MAU (Monthly Active Users) - refresh_token.last_used_at 기준 (회원만)
     * 월별 고유 사용자 수 조회
     * status 무관하게 last_used_at이 기간 내에 있으면 활성 사용자로 카운트
     */
    @Query(value = """
        SELECT DATE_FORMAT(last_used_at, '%Y-%m') AS month, COUNT(DISTINCT users_id) AS mau
        FROM refresh_token
        WHERE last_used_at >= :startDate AND last_used_at < :endDate
          AND user_type = 'USER'
        GROUP BY DATE_FORMAT(last_used_at, '%Y-%m')
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlyActiveUsers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * MAU 카운트 (기간 내 고유 활성 사용자 수)
     * users.status = 'ACTIVE'인 유저만 카운트
     */
    @Query(value = """
        SELECT COUNT(DISTINCT rt.users_id)
        FROM refresh_token rt
        JOIN users u ON rt.users_id = u.users_id
        WHERE rt.last_used_at >= :startDate AND rt.last_used_at < :endDate
          AND rt.user_type = 'USER'
          AND u.status = 'ACTIVE'
        """, nativeQuery = true)
    Long countMau(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 시간대별 접속 분포
     */
    @Query(value = """
        SELECT HOUR(last_used_at) AS hour, COUNT(*) AS count
        FROM refresh_token
        WHERE last_used_at >= :startDate AND last_used_at < :endDate
          AND user_type = 'USER'
        GROUP BY HOUR(last_used_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> findHourlyAccessDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 휴면 유저 분석 ====================

    /**
     * 휴면 유저 수 (마케팅 동의자만, 기간별 필터)
     * refresh_token 기준: last_used_at이 NULL이면 issued_at 사용, 둘 다 NULL이면 created_date 사용
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id) AS dormant_count
        FROM users u
        LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.created_date >= :startDate AND u.created_date < :endDate
        GROUP BY u.users_id, u.created_date
        HAVING DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) >= :days
        """, nativeQuery = true)
    List<Object[]> countDormantUsersWithMarketing(
            @Param("days") int days,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 휴면 기간별 분포 (대시보드용)
     * 7일+, 30일+, 90일+ 기준
     * 기간 내 가입한 유저 기준
     * last_used_at이 NULL이면 issued_at 사용, 둘 다 NULL이면 created_date 사용
     */
    @Query(value = """
        SELECT
            CASE
                WHEN days_inactive >= 90 THEN '90+'
                WHEN days_inactive >= 30 THEN '30+'
                WHEN days_inactive >= 7 THEN '7+'
                ELSE 'active'
            END AS dormant_group,
            COUNT(*) AS user_count
        FROM (
            SELECT u.users_id,
                   DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) AS days_inactive
            FROM users u
            LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
            GROUP BY u.users_id, u.created_date
        ) sub
        GROUP BY dormant_group
        ORDER BY
            CASE dormant_group
                WHEN 'active' THEN 1
                WHEN '7+' THEN 2
                WHEN '30+' THEN 3
                WHEN '90+' THEN 4
            END
        """, nativeQuery = true)
    List<Object[]> findDormantUserDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 휴면 유저 상세 리스트 (리텐션용 - 모든 유저)
     * last_used_at이 NULL이면 issued_at 사용, 둘 다 NULL이면 created_date 사용
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date) AS last_login,
               DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) AS days_inactive
        FROM users u
        LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.created_date >= :startDate AND u.created_date < :endDate
        GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
                 u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
                 u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        HAVING days_inactive >= :days
        ORDER BY days_inactive DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findDormantUserDetailsForRetention(
            @Param("days") int days,
            @Param("limit") int limit,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 휴면 유저 수 (리텐션용 - 모든 유저)
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
            GROUP BY u.users_id, u.created_date
            HAVING DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) >= :days
        ) sub
        """, nativeQuery = true)
    Long countDormantUsersForRetention(
            @Param("days") int days,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입 유저의 휴면율 (60일+ 미접속)
     * 반환: [dormant_count, total_count]
     * last_used_at이 NULL이면 issued_at 사용, 둘 다 NULL이면 created_date 사용
     */
    @Query(value = """
        SELECT
            SUM(CASE WHEN days_inactive >= 60 THEN 1 ELSE 0 END) AS dormant_count,
            COUNT(*) AS total_count
        FROM (
            SELECT u.users_id,
                   DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) AS days_inactive
            FROM users u
            LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
            GROUP BY u.users_id, u.created_date
        ) sub
        """, nativeQuery = true)
    List<Object[]> countDormantRateByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 휴면 유저 상세 리스트 (마케팅 동의자만, 기간 필터)
     * last_used_at이 NULL이면 issued_at 사용, 둘 다 NULL이면 created_date 사용
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date) AS last_login,
               DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) AS days_inactive
        FROM users u
        LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.created_date >= :startDate AND u.created_date < :endDate
        GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
                 u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
                 u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        HAVING days_inactive >= :days
        ORDER BY days_inactive DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findDormantUserDetails(
            @Param("days") int days,
            @Param("limit") int limit,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 마케팅 타겟 ====================

    /**
     * 마케팅 동의 유저 상세 리스트
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        FROM users u
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
        ORDER BY u.created_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findMarketingAgreedUserDetails(@Param("limit") int limit);

    /**
     * 가입 후 이벤트 미생성 유저 수 (마케팅 동의자만)
     * 가입 후 지정 일수 경과했지만 이벤트를 생성하지 않은 유저
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        LEFT JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND ep.users_id IS NULL
          AND u.created_date < DATE_SUB(NOW(), INTERVAL :daysAfterSignup DAY)
        """, nativeQuery = true)
    Long countNoEventUsers(@Param("daysAfterSignup") int daysAfterSignup);

    /**
     * 가입 후 이벤트 미생성 유저 상세 리스트
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               DATEDIFF(NOW(), u.created_date) AS days_since_signup
        FROM users u
        LEFT JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND ep.users_id IS NULL
          AND u.created_date < DATE_SUB(NOW(), INTERVAL :daysAfterSignup DAY)
        ORDER BY u.created_date
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNoEventUserDetails(
            @Param("daysAfterSignup") int daysAfterSignup,
            @Param("limit") int limit
    );

    /**
     * 1회성 유저 수 (이벤트 1개만 생성, 마케팅 동의자만)
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
                AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            WHERE u.status = 'ACTIVE'
              AND u.marketing_policy_agreement = 1
            GROUP BY u.users_id
            HAVING COUNT(ep.events_id) = 1
        ) AS one_time_users
        """, nativeQuery = true)
    Long countOneTimeUsers();

    /**
     * 1회성 유저 상세 리스트
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               COUNT(ep.events_id) AS event_count
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
        GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
                 u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
                 u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        HAVING COUNT(ep.events_id) = 1
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findOneTimeUserDetails(@Param("limit") int limit);

    /**
     * VIP 유저 수 (이벤트 5개+ 생성)
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
                AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            WHERE u.status = 'ACTIVE'
            GROUP BY u.users_id
            HAVING COUNT(ep.events_id) >= 5
        ) AS vip_users
        """, nativeQuery = true)
    Long countVipUsers();

    /**
     * VIP 유저 상세 리스트
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               COUNT(ep.events_id) AS event_count
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
        GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
                 u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
                 u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        HAVING COUNT(ep.events_id) >= 5
        ORDER BY event_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findVipUserDetails(@Param("limit") int limit);

    /**
     * 참여자 0명 이벤트 수 (3일 이상 경과)
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT e.events_id
            FROM events e
            JOIN event_participations ep ON e.events_id = ep.events_id
            LEFT JOIN members m ON e.events_id = m.events_id
            WHERE ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
              AND e.status = 'ACTIVE'
              AND e.created_date < DATE_SUB(NOW(), INTERVAL 3 DAY)
            GROUP BY e.events_id
            HAVING COUNT(m.members_id) = 0
        ) AS zero_participant_events
        """, nativeQuery = true)
    Long countZeroParticipantEvents();

    // ==================== 날짜 범위 필터 버전 (마케팅 타겟) ====================

    /**
     * 기간 내 가입한 유저 중 마케팅 동의 유저 수
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM users u
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.created_date >= :startDate
          AND u.created_date < :endDate
        """, nativeQuery = true)
    Long countMarketingAgreedUsersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입한 유저 중 휴면 유저 수 (30일+ 미접속)
     * last_used_at이 NULL이면 issued_at 사용
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.created_date >= :startDate
          AND u.created_date < :endDate
          AND COALESCE(rt.last_used_at, rt.issued_at) < DATE_SUB(NOW(), INTERVAL 30 DAY)
        """, nativeQuery = true)
    Long countDormantUsersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입한 유저 중 이벤트 미생성 유저 수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        LEFT JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND ep.users_id IS NULL
          AND u.created_date >= :startDate
          AND u.created_date < :endDate
          AND u.created_date < DATE_SUB(NOW(), INTERVAL 7 DAY)
        """, nativeQuery = true)
    Long countNoEventUsersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입한 유저 중 일회성 유저 수
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
                AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            WHERE u.status = 'ACTIVE'
              AND u.marketing_policy_agreement = 1
              AND u.created_date >= :startDate
              AND u.created_date < :endDate
            GROUP BY u.users_id
            HAVING COUNT(ep.events_id) = 1
        ) AS one_time_users
        """, nativeQuery = true)
    Long countOneTimeUsersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입한 유저 중 VIP 유저 수
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
                AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate
              AND u.created_date < :endDate
            GROUP BY u.users_id
            HAVING COUNT(ep.events_id) >= 5
        ) AS vip_users
        """, nativeQuery = true)
    Long countVipUsersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 생성된 이벤트 중 참여자 0명 이벤트 수
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT e.events_id
            FROM events e
            JOIN event_participations ep ON e.events_id = ep.events_id
            LEFT JOIN members m ON e.events_id = m.events_id
            WHERE ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
              AND e.status = 'ACTIVE'
              AND e.created_date >= :startDate
              AND e.created_date < :endDate
              AND e.created_date < DATE_SUB(NOW(), INTERVAL 3 DAY)
            GROUP BY e.events_id
            HAVING COUNT(m.members_id) = 0
        ) AS zero_participant_events
        """, nativeQuery = true)
    Long countZeroParticipantEventsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 참여자 0명 이벤트 상세 리스트
     */
    @Query(value = """
        SELECT e.events_id, e.title, e.category, e.start_time, e.end_time, e.created_date,
               u.users_id, u.email, u.name, u.nickname,
               DATEDIFF(NOW(), e.created_date) AS days_since_created
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        JOIN events e ON ep.events_id = e.events_id
        LEFT JOIN members m ON e.events_id = m.events_id
        WHERE ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
          AND e.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND e.created_date < DATE_SUB(NOW(), INTERVAL 3 DAY)
        GROUP BY e.events_id, e.title, e.category, e.start_time, e.end_time, e.created_date,
                 u.users_id, u.email, u.name, u.nickname
        HAVING COUNT(m.members_id) = 0
        ORDER BY e.created_date
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findZeroParticipantEventDetails(@Param("limit") int limit);

    // ==================== 월별 가입자 (Native Query) ====================

    /**
     * 월별 신규 가입 유저 수
     */
    @Query(value = """
        SELECT DATE_FORMAT(created_date, '%Y-%m') AS month,
               COUNT(*) AS new_users,
               SUM(CASE WHEN status = 'DELETED' THEN 1 ELSE 0 END) AS deleted_count
        FROM users
        WHERE created_date >= :startDate AND created_date < :endDate
        GROUP BY DATE_FORMAT(created_date, '%Y-%m')
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlySignups(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 재방문율 분석 ====================

    /**
     * 재방문 유저 수 (이벤트 2개+ 참여)
     * 기간 내 가입한 유저 기준
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
            GROUP BY u.users_id
            HAVING COUNT(DISTINCT ep.events_id) >= 2
        ) AS returning_users
        """, nativeQuery = true)
    Long countReturningUsers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 재방문 유저 상세 목록 (이벤트 2개+ 참여)
     * 기간 내 가입한 유저 기준
     */
    @Query(value = """
        SELECT u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
               u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
               u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date,
               COUNT(DISTINCT ep.events_id) AS participation_count
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        WHERE u.status = 'ACTIVE'
          AND u.created_date >= :startDate AND u.created_date < :endDate
        GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
                 u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
                 u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        HAVING COUNT(DISTINCT ep.events_id) >= 2
        ORDER BY participation_count DESC, u.created_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findReturningUserDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit
    );

    /**
     * 이벤트 참여 경험이 있는 총 유저 수
     * 기간 내 가입한 유저 기준
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        WHERE u.status = 'ACTIVE'
          AND u.created_date >= :startDate AND u.created_date < :endDate
        """, nativeQuery = true)
    Long countUsersWithEvents(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 가입 후 첫 이벤트 생성까지 소요 일수 ====================

    /**
     * 평균 가입 후 첫 이벤트 생성까지 소요 일수
     * 기간 내 가입한 유저 기준
     */
    @Query(value = """
        SELECT AVG(days_to_first_event) FROM (
            SELECT DATEDIFF(MIN(e.created_date), u.created_date) AS days_to_first_event
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
                AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            JOIN events e ON ep.events_id = e.events_id
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
            GROUP BY u.users_id, u.created_date
            HAVING MIN(e.created_date) IS NOT NULL
        ) AS first_event_days
        """, nativeQuery = true)
    Double findAverageDaysToFirstEvent(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 대시보드 성능 개선용 쿼리 ====================

    /**
     * 기간별 유저 통계 (한 번에 조회)
     * totalUsers, marketingAgreed, google, kakao, naver 카운트
     */
    @Query(value = """
        SELECT
            COUNT(*) AS total_users,
            SUM(CASE WHEN marketing_policy_agreement = 1 THEN 1 ELSE 0 END) AS marketing_agreed,
            SUM(CASE WHEN provider = 'google' THEN 1 ELSE 0 END) AS google_count,
            SUM(CASE WHEN provider = 'kakao' THEN 1 ELSE 0 END) AS kakao_count,
            SUM(CASE WHEN provider = 'naver' THEN 1 ELSE 0 END) AS naver_count
        FROM users
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        """, nativeQuery = true)
    List<Object[]> getUserStatsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간별 이벤트 통계 (한 번에 조회)
     * totalEvents, DATE 카테고리, DAY 카테고리 카운트
     */
    @Query(value = """
        SELECT
            COUNT(*) AS total_events,
            SUM(CASE WHEN category = 'DATE' THEN 1 ELSE 0 END) AS date_count,
            SUM(CASE WHEN category = 'DAY' THEN 1 ELSE 0 END) AS day_count
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        """, nativeQuery = true)
    List<Object[]> getEventStatsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Provider별 유저 분포
     */
    @Query(value = """
        SELECT provider, COUNT(*) AS count
        FROM users
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY provider
        """, nativeQuery = true)
    List<Object[]> getProviderDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 언어별 유저 분포
     */
    @Query(value = """
        SELECT language, COUNT(*) AS count
        FROM users
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY language
        """, nativeQuery = true)
    List<Object[]> getLanguageDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 요일별 이벤트 분포 (1=일요일, 2=월요일, ..., 7=토요일)
     */
    @Query(value = """
        SELECT DAYOFWEEK(created_date) AS day_of_week, COUNT(*) AS count
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY DAYOFWEEK(created_date)
        ORDER BY day_of_week
        """, nativeQuery = true)
    List<Object[]> getEventWeekdayDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 카테고리별 이벤트 분포
     */
    @Query(value = """
        SELECT category, COUNT(*) AS count
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY category
        """, nativeQuery = true)
    List<Object[]> getCategoryDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 유저별 이벤트 참여 수 (배치 조회)
     * AdminService.getAllDashboardUsers()에서 사용
     */
    @Query(value = """
        SELECT u.users_id, COUNT(ep.event_participations_id) AS participation_count
        FROM users u
        LEFT JOIN event_participations ep ON u.users_id = ep.users_id
        WHERE u.users_id IN :userIds
        GROUP BY u.users_id
        """, nativeQuery = true)
    List<Object[]> countParticipationsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 이벤트별 참여자 수 (회원 + 비회원) - 배치 조회
     * AdminService.getAllDashboardEvents()에서 사용
     */
    @Query(value = """
        SELECT
            e.events_id,
            COALESCE(ep_sub.ep_count, 0) + COALESCE(m_sub.m_count, 0) AS participant_count
        FROM events e
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS ep_count
            FROM event_participations
            WHERE event_status NOT IN ('CREATOR')
            GROUP BY events_id
        ) ep_sub ON e.events_id = ep_sub.events_id
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS m_count
            FROM members
            GROUP BY events_id
        ) m_sub ON e.events_id = m_sub.events_id
        WHERE e.events_id IN :eventIds
        """, nativeQuery = true)
    List<Object[]> countParticipantsByEventIds(@Param("eventIds") List<Long> eventIds);

    /**
     * 참여자 수 기준 정렬된 이벤트 ID 목록 (페이징)
     */
    @Query(value = """
        SELECT
            e.events_id,
            COALESCE(ep_sub.ep_count, 0) + COALESCE(m_sub.m_count, 0) AS participant_count
        FROM events e
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS ep_count
            FROM event_participations
            WHERE event_status NOT IN ('CREATOR')
            GROUP BY events_id
        ) ep_sub ON e.events_id = ep_sub.events_id
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS m_count
            FROM members
            GROUP BY events_id
        ) m_sub ON e.events_id = m_sub.events_id
        WHERE e.status = 'ACTIVE'
        ORDER BY participant_count DESC, e.events_id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findEventIdsByParticipantCountDesc(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 참여자 수 기준 정렬된 이벤트 ID 목록 (오름차순, 페이징)
     */
    @Query(value = """
        SELECT
            e.events_id,
            COALESCE(ep_sub.ep_count, 0) + COALESCE(m_sub.m_count, 0) AS participant_count
        FROM events e
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS ep_count
            FROM event_participations
            WHERE event_status NOT IN ('CREATOR')
            GROUP BY events_id
        ) ep_sub ON e.events_id = ep_sub.events_id
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS m_count
            FROM members
            GROUP BY events_id
        ) m_sub ON e.events_id = m_sub.events_id
        WHERE e.status = 'ACTIVE'
        ORDER BY participant_count ASC, e.events_id ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findEventIdsByParticipantCountAsc(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 이벤트 키워드 분석 (타이틀에서 키워드 포함 여부)
     * 30개 키워드 지원
     */
    @Query(value = """
        SELECT
            SUM(CASE WHEN title LIKE '%회의%' THEN 1 ELSE 0 END) AS keyword_meeting,
            SUM(CASE WHEN title LIKE '%스터디%' THEN 1 ELSE 0 END) AS keyword_study,
            SUM(CASE WHEN title LIKE '%밥%' THEN 1 ELSE 0 END) AS keyword_meal,
            SUM(CASE WHEN title LIKE '%술%' THEN 1 ELSE 0 END) AS keyword_drink,
            SUM(CASE WHEN title LIKE '%MT%' OR title LIKE '%mt%' THEN 1 ELSE 0 END) AS keyword_mt,
            SUM(CASE WHEN title LIKE '%면접%' THEN 1 ELSE 0 END) AS keyword_interview,
            SUM(CASE WHEN title LIKE '%프로젝트%' THEN 1 ELSE 0 END) AS keyword_project,
            SUM(CASE WHEN title LIKE '%동아리%' THEN 1 ELSE 0 END) AS keyword_club,
            SUM(CASE WHEN title LIKE '%모임%' THEN 1 ELSE 0 END) AS keyword_gathering,
            SUM(CASE WHEN title LIKE '%여행%' THEN 1 ELSE 0 END) AS keyword_travel,
            SUM(CASE WHEN title LIKE '%점심%' THEN 1 ELSE 0 END) AS keyword_lunch,
            SUM(CASE WHEN title LIKE '%저녁%' THEN 1 ELSE 0 END) AS keyword_dinner,
            SUM(CASE WHEN title LIKE '%식사%' THEN 1 ELSE 0 END) AS keyword_dining,
            SUM(CASE WHEN title LIKE '%커피%' THEN 1 ELSE 0 END) AS keyword_coffee,
            SUM(CASE WHEN title LIKE '%미팅%' THEN 1 ELSE 0 END) AS keyword_meeting2,
            SUM(CASE WHEN title LIKE '%팀%' THEN 1 ELSE 0 END) AS keyword_team,
            SUM(CASE WHEN title LIKE '%워크샵%' THEN 1 ELSE 0 END) AS keyword_workshop,
            SUM(CASE WHEN title LIKE '%세미나%' THEN 1 ELSE 0 END) AS keyword_seminar,
            SUM(CASE WHEN title LIKE '%강의%' THEN 1 ELSE 0 END) AS keyword_lecture,
            SUM(CASE WHEN title LIKE '%수업%' THEN 1 ELSE 0 END) AS keyword_class,
            SUM(CASE WHEN title LIKE '%운동%' THEN 1 ELSE 0 END) AS keyword_exercise,
            SUM(CASE WHEN title LIKE '%헬스%' THEN 1 ELSE 0 END) AS keyword_fitness,
            SUM(CASE WHEN title LIKE '%축구%' THEN 1 ELSE 0 END) AS keyword_soccer,
            SUM(CASE WHEN title LIKE '%농구%' THEN 1 ELSE 0 END) AS keyword_basketball,
            SUM(CASE WHEN title LIKE '%야구%' THEN 1 ELSE 0 END) AS keyword_baseball,
            SUM(CASE WHEN title LIKE '%테니스%' THEN 1 ELSE 0 END) AS keyword_tennis,
            SUM(CASE WHEN title LIKE '%골프%' THEN 1 ELSE 0 END) AS keyword_golf,
            SUM(CASE WHEN title LIKE '%등산%' THEN 1 ELSE 0 END) AS keyword_hiking,
            SUM(CASE WHEN title LIKE '%캠핑%' THEN 1 ELSE 0 END) AS keyword_camping,
            SUM(CASE WHEN title LIKE '%파티%' THEN 1 ELSE 0 END) AS keyword_party
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        """, nativeQuery = true)
    List<Object[]> getKeywordCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 평균 참여자 수 계산 (기간별)
     */
    @Query(value = """
        SELECT
            COUNT(DISTINCT e.events_id) AS event_count,
            COUNT(DISTINCT ep.event_participations_id) AS ep_count,
            COUNT(DISTINCT m.members_id) AS member_count
        FROM events e
        LEFT JOIN event_participations ep ON e.events_id = ep.events_id
            AND ep.event_status NOT IN ('CREATOR')
        LEFT JOIN members m ON e.events_id = m.events_id
        WHERE e.status = 'ACTIVE'
          AND e.created_date >= :startDate AND e.created_date < :endDate
        """, nativeQuery = true)
    List<Object[]> getAvgParticipantsData(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 월별 이벤트 생성 수
     */
    @Query(value = """
        SELECT DATE_FORMAT(created_date, '%Y-%m') AS month, COUNT(*) AS event_count
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY DATE_FORMAT(created_date, '%Y-%m')
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlyEvents(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 이메일 발송용 쿼리 ====================

    /**
     * 마케팅 동의 유저 이메일 목록
     */
    @Query(value = """
        SELECT email FROM users
        WHERE status = 'ACTIVE'
          AND marketing_policy_agreement = 1
          AND email IS NOT NULL
        ORDER BY created_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findMarketingAgreedUserEmails(@Param("limit") int limit);

    /**
     * 휴면 유저 이메일 목록 (마케팅 동의자만)
     * last_used_at이 NULL이면 issued_at 사용
     */
    @Query(value = """
        SELECT u.email
        FROM users u
        LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.email IS NOT NULL
        GROUP BY u.users_id, u.email
        HAVING DATEDIFF(NOW(), MAX(COALESCE(rt.last_used_at, rt.issued_at))) >= :days
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findDormantUserEmails(
            @Param("days") int days,
            @Param("limit") int limit
    );

    /**
     * 이벤트 미생성 유저 이메일 목록
     */
    @Query(value = """
        SELECT u.email
        FROM users u
        LEFT JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.email IS NOT NULL
          AND ep.users_id IS NULL
          AND u.created_date < DATE_SUB(NOW(), INTERVAL :daysAfterSignup DAY)
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findNoEventUserEmails(
            @Param("daysAfterSignup") int daysAfterSignup,
            @Param("limit") int limit
    );

    /**
     * 1회성 유저 이메일 목록
     */
    @Query(value = """
        SELECT u.email
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.marketing_policy_agreement = 1
          AND u.email IS NOT NULL
        GROUP BY u.users_id, u.email
        HAVING COUNT(ep.events_id) = 1
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findOneTimeUserEmails(@Param("limit") int limit);

    /**
     * VIP 유저 이메일 목록 (이벤트 5개+ 생성)
     */
    @Query(value = """
        SELECT u.email
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
            AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        WHERE u.status = 'ACTIVE'
          AND u.email IS NOT NULL
        GROUP BY u.users_id, u.email
        HAVING COUNT(ep.events_id) >= 5
        ORDER BY COUNT(ep.events_id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findVipUserEmails(@Param("limit") int limit);

    // ==================== 유저 검색 (이메일 발송용) ====================

    /**
     * 이름 또는 이메일로 유저 검색
     */
    @Query(value = """
        SELECT u.users_id, u.name, u.email, u.nickname, u.provider
        FROM users u
        WHERE u.status = 'ACTIVE'
          AND u.email IS NOT NULL
          AND (u.name LIKE CONCAT('%', :query, '%')
               OR u.email LIKE CONCAT('%', :query, '%')
               OR u.nickname LIKE CONCAT('%', :query, '%'))
        ORDER BY u.created_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> searchUsersByNameOrEmail(@Param("query") String query, @Param("limit") int limit);

    // ==================== 전환 퍼널 분석 ====================

    /**
     * Step 1: 총 가입자 수 (기간 내)
     */
    @Query(value = """
        SELECT COUNT(*) FROM users
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        """, nativeQuery = true)
    Long countSignups(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Step 2: 첫 이벤트 생성 유저 수
     * 해당 기간 가입자 중 이벤트를 1개 이상 생성한 유저
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        WHERE u.status = 'ACTIVE'
          AND u.created_date >= :startDate AND u.created_date < :endDate
          AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        """, nativeQuery = true)
    Long countUsersWithFirstEvent(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Step 3: 참여자 1명 이상 받은 유저 수
     * 이벤트 생성 후 members 또는 PARTICIPANT가 1명 이상 있는 이벤트의 생성자
     */
    @Query(value = """
        SELECT COUNT(DISTINCT creator_user_id) FROM (
            SELECT ep_creator.users_id AS creator_user_id
            FROM users u
            JOIN event_participations ep_creator ON u.users_id = ep_creator.users_id
            JOIN events e ON ep_creator.events_id = e.events_id
            LEFT JOIN event_participations ep_participant ON e.events_id = ep_participant.events_id
                AND ep_participant.event_status = 'PARTICIPANT'
            LEFT JOIN members m ON e.events_id = m.events_id
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
              AND ep_creator.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
              AND e.status = 'ACTIVE'
            GROUP BY ep_creator.users_id, e.events_id
            HAVING COUNT(ep_participant.event_participations_id) > 0
                OR COUNT(m.members_id) > 0
        ) AS users_with_participants
        """, nativeQuery = true)
    Long countUsersWithParticipants(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Step 4: 2번째 이벤트 생성 유저 수
     * 이벤트를 2개 이상 생성한 유저
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT u.users_id
            FROM users u
            JOIN event_participations ep ON u.users_id = ep.users_id
            WHERE u.status = 'ACTIVE'
              AND u.created_date >= :startDate AND u.created_date < :endDate
              AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
            GROUP BY u.users_id
            HAVING COUNT(DISTINCT ep.events_id) >= 2
        ) AS users_with_two_events
        """, nativeQuery = true)
    Long countUsersWithSecondEvent(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 코호트 리텐션 분석 ====================

    /**
     * 코호트별 가입자 수 (월별)
     */
    @Query(value = """
        SELECT DATE_FORMAT(created_date, '%Y-%m') AS cohort_month,
               COUNT(*) AS cohort_size
        FROM users
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY DATE_FORMAT(created_date, '%Y-%m')
        ORDER BY cohort_month
        """, nativeQuery = true)
    List<Object[]> findCohortSizes(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 코호트별 월별 활성 유저 수
     * refresh_token.last_used_at 기준 (status 무관)
     */
    @Query(value = """
        SELECT
            DATE_FORMAT(u.created_date, '%Y-%m') AS cohort_month,
            DATE_FORMAT(rt.last_used_at, '%Y-%m') AS active_month,
            COUNT(DISTINCT u.users_id) AS active_users
        FROM users u
        JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'
        WHERE u.status = 'ACTIVE'
          AND u.created_date >= :startDate AND u.created_date < :endDate
          AND rt.last_used_at >= u.created_date
        GROUP BY DATE_FORMAT(u.created_date, '%Y-%m'), DATE_FORMAT(rt.last_used_at, '%Y-%m')
        ORDER BY cohort_month, active_month
        """, nativeQuery = true)
    List<Object[]> findCohortMonthlyActivity(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== TTV (Time to Value) 분석 ====================

    /**
     * 유저별 첫 이벤트 생성까지 걸린 일수 조회
     * TTV = 첫 이벤트 생성일 - 가입일
     */
    @Query(value = """
        SELECT DATEDIFF(MIN(e.created_date), u.created_date) AS days_to_first_event
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        JOIN events e ON ep.events_id = e.events_id
        WHERE u.status = 'ACTIVE'
          AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
          AND u.created_date >= :startDate AND u.created_date < :endDate
        GROUP BY u.users_id
        HAVING days_to_first_event IS NOT NULL
        ORDER BY days_to_first_event
        """, nativeQuery = true)
    List<Integer> findTtvDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 기간 내 가입자 중 이벤트 생성한 유저 수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.users_id)
        FROM users u
        JOIN event_participations ep ON u.users_id = ep.users_id
        WHERE u.status = 'ACTIVE'
          AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
          AND u.created_date >= :startDate AND u.created_date < :endDate
        """, nativeQuery = true)
    Long countUsersWithAnyEvent(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== Time × Weekday Heatmap ====================

    /**
     * 이벤트 생성 시간대(0-23) × 요일(1-7, 일=1) 분포
     * 반환: [hour, dayOfWeek, count]
     */
    @Query(value = """
        SELECT HOUR(created_date) AS hour_of_day,
               DAYOFWEEK(created_date) AS day_of_week,
               COUNT(*) AS event_count
        FROM events
        WHERE status = 'ACTIVE'
          AND created_date >= :startDate AND created_date < :endDate
        GROUP BY HOUR(created_date), DAYOFWEEK(created_date)
        ORDER BY hour_of_day, day_of_week
        """, nativeQuery = true)
    List<Object[]> findEventCreationHeatmap(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== WAU/MAU Stickiness ====================

    /**
     * 주간 활성 유저 수 (WAU)
     * users.status = 'ACTIVE'인 유저만 카운트
     */
    @Query(value = """
        SELECT COUNT(DISTINCT rt.users_id)
        FROM refresh_token rt
        JOIN users u ON rt.users_id = u.users_id
        WHERE rt.last_used_at >= :startDate AND rt.last_used_at < :endDate
          AND rt.user_type = 'USER'
          AND u.status = 'ACTIVE'
        """, nativeQuery = true)
    Long countWau(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 월별 WAU 트렌드 (각 월의 주 평균 WAU)
     * users.status = 'ACTIVE'인 유저만 카운트
     * 반환: [month, avg_wau]
     */
    @Query(value = """
        SELECT DATE_FORMAT(week_start, '%Y-%m') AS month,
               AVG(weekly_users) AS avg_wau
        FROM (
            SELECT DATE(DATE_SUB(rt.last_used_at, INTERVAL WEEKDAY(rt.last_used_at) DAY)) AS week_start,
                   COUNT(DISTINCT rt.users_id) AS weekly_users
            FROM refresh_token rt
            JOIN users u ON rt.users_id = u.users_id
            WHERE rt.last_used_at >= :startDate AND rt.last_used_at < :endDate
              AND rt.user_type = 'USER'
              AND u.status = 'ACTIVE'
            GROUP BY week_start
        ) AS weekly_data
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlyAvgWau(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 월별 MAU
     * users.status = 'ACTIVE'인 유저만 카운트
     * 반환: [month, mau]
     */
    @Query(value = """
        SELECT DATE_FORMAT(rt.last_used_at, '%Y-%m') AS month,
               COUNT(DISTINCT rt.users_id) AS mau
        FROM refresh_token rt
        JOIN users u ON rt.users_id = u.users_id
        WHERE rt.last_used_at >= :startDate AND rt.last_used_at < :endDate
          AND rt.user_type = 'USER'
          AND u.status = 'ACTIVE'
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlyMau(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== 유저 상세 정보 ====================

    /**
     * 유저 기본 정보 조회
     * 반환: [name, nickname, email, provider, language, created_date, marketing_agreement]
     */
    @Query(value = """
        SELECT u.name, u.nickname, u.email, u.provider, u.language, u.created_date, u.marketing_policy_agreement
        FROM users u
        WHERE u.users_id = :userId AND u.status = 'ACTIVE'
        """, nativeQuery = true)
    List<Object[]> findUserBasicInfo(@Param("userId") Long userId);

    /**
     * 유저의 refresh_token 정보 조회
     * 반환: [last_used_at, last_used_ip, active_count]
     */
    @Query(value = """
        SELECT MAX(rt.last_used_at) AS last_used_at,
               (SELECT rt2.last_used_ip FROM refresh_token rt2
                WHERE rt2.users_id = :userId
                ORDER BY rt2.last_used_at DESC LIMIT 1) AS last_used_ip,
               SUM(CASE WHEN rt.status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count
        FROM refresh_token rt
        WHERE rt.users_id = :userId
        """, nativeQuery = true)
    List<Object[]> findUserTokenInfo(@Param("userId") Long userId);

    /**
     * 유저가 생성한 이벤트 수
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM event_participations ep
        WHERE ep.users_id = :userId
          AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
        """, nativeQuery = true)
    Long countUserCreatedEvents(@Param("userId") Long userId);

    /**
     * 유저가 참여한 이벤트 수 (생성 제외)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM event_participations ep
        WHERE ep.users_id = :userId
          AND ep.event_status = 'PARTICIPANT'
        """, nativeQuery = true)
    Long countUserParticipatedEvents(@Param("userId") Long userId);

    // ==================== CSV 내보내기용 경량 쿼리 ====================

    /**
     * CSV 내보내기용 유저 목록 (경량)
     * 반환: [users_id, name, email, nickname, provider, language, marketing_policy_agreement, created_date, participation_count]
     */
    @Query(value = """
        SELECT
            u.users_id,
            u.name,
            u.email,
            u.nickname,
            u.provider,
            u.language,
            u.marketing_policy_agreement,
            u.created_date,
            COALESCE(p.participation_count, 0) AS participation_count
        FROM users u
        LEFT JOIN (
            SELECT ep.users_id, COUNT(*) AS participation_count
            FROM event_participations ep
            WHERE ep.event_status NOT IN ('CREATOR')
            GROUP BY ep.users_id
        ) p ON u.users_id = p.users_id
        WHERE u.status = 'ACTIVE'
          AND (:search IS NULL OR u.name LIKE CONCAT('%', :search, '%') OR u.email LIKE CONCAT('%', :search, '%'))
          AND (:startDate IS NULL OR u.created_date >= :startDate)
          AND (:endDate IS NULL OR u.created_date < :endDate)
        ORDER BY u.created_date DESC
        """, nativeQuery = true)
    List<Object[]> findUsersForCsvExport(
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * CSV 내보내기용 이벤트 목록 (경량)
     * 반환: [events_id, events_uuid, title, category, start_time, end_time, created_date, participant_count]
     */
    @Query(value = """
        SELECT
            e.events_id,
            HEX(e.events_uuid) AS events_uuid,
            e.title,
            e.category,
            e.start_time,
            e.end_time,
            e.created_date,
            COALESCE(ep_sub.ep_count, 0) + COALESCE(m_sub.m_count, 0) AS participant_count
        FROM events e
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS ep_count
            FROM event_participations
            WHERE event_status NOT IN ('CREATOR')
            GROUP BY events_id
        ) ep_sub ON e.events_id = ep_sub.events_id
        LEFT JOIN (
            SELECT events_id, COUNT(*) AS m_count
            FROM members
            GROUP BY events_id
        ) m_sub ON e.events_id = m_sub.events_id
        WHERE e.status = 'ACTIVE'
          AND (:search IS NULL OR e.title LIKE CONCAT('%', :search, '%'))
          AND (:startDate IS NULL OR e.created_date >= :startDate)
          AND (:endDate IS NULL OR e.created_date < :endDate)
        ORDER BY e.created_date DESC
        """, nativeQuery = true)
    List<Object[]> findEventsForCsvExport(
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
