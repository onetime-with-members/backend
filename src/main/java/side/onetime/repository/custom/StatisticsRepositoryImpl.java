package side.onetime.repository.custom;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

/**
 * 통계 Repository 커스텀 구현체
 * Native Query로 정렬, 검색, 기간 필터 기능 지원
 */
@Repository
@RequiredArgsConstructor
public class StatisticsRepositoryImpl implements StatisticsRepositoryCustom {

    private final EntityManager entityManager;

    private static final String USER_SELECT_COLUMNS = """
        u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,
        u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,
        u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date
        """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findMarketingAgreedUserDetailsWithSortAndSearch(String sort, String search,
                                                                           LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(USER_SELECT_COLUMNS);
        sql.append(" FROM users u");
        sql.append(" WHERE u.status = 'ACTIVE' AND u.marketing_policy_agreement = 1");
        sql.append(" AND u.created_date >= :startDate AND u.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.name LIKE :search OR u.email LIKE :search OR u.nickname LIKE :search)");
        }

        sql.append(" ORDER BY ").append(getSortClause(sort, "u", SortContext.USER));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findDormantUserDetailsWithSortAndSearch(int days, String sort, String search,
                                                                   LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(USER_SELECT_COLUMNS);
        sql.append(", COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date) AS last_login");
        sql.append(", DATEDIFF(NOW(), COALESCE(MAX(COALESCE(rt.last_used_at, rt.issued_at)), u.created_date)) AS days_inactive");
        sql.append(" FROM users u");
        sql.append(" LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.user_type = 'USER'");
        sql.append(" WHERE u.status = 'ACTIVE'");
        sql.append(" AND u.created_date >= :startDate AND u.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.name LIKE :search OR u.email LIKE :search OR u.nickname LIKE :search)");
        }

        sql.append(" GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,");
        sql.append(" u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,");
        sql.append(" u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date");
        sql.append(" HAVING days_inactive >= :days");
        sql.append(" ORDER BY ").append(getSortClause(sort, "u", SortContext.USER));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("days", days);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findNoEventUserDetailsWithSortAndSearch(int daysAfterSignup, String sort, String search,
                                                                   LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(USER_SELECT_COLUMNS);
        sql.append(", DATEDIFF(NOW(), u.created_date) AS days_since_signup");
        sql.append(" FROM users u");
        sql.append(" LEFT JOIN event_participations ep ON u.users_id = ep.users_id");
        sql.append("   AND ep.participation_role IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')");
        sql.append(" WHERE u.status = 'ACTIVE'");
        sql.append("   AND ep.users_id IS NULL");
        sql.append("   AND u.created_date < DATE_SUB(NOW(), INTERVAL :daysAfterSignup DAY)");
        sql.append("   AND u.created_date >= :startDate AND u.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.name LIKE :search OR u.email LIKE :search OR u.nickname LIKE :search)");
        }

        sql.append(" ORDER BY ").append(getSortClause(sort, "u", SortContext.USER));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("daysAfterSignup", daysAfterSignup);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findOneTimeUserDetailsWithSortAndSearch(String sort, String search,
                                                                   LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(USER_SELECT_COLUMNS);
        sql.append(", COUNT(ep.events_id) AS event_count");
        sql.append(" FROM users u");
        sql.append(" JOIN event_participations ep ON u.users_id = ep.users_id");
        sql.append("   AND ep.participation_role IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')");
        sql.append(" WHERE u.status = 'ACTIVE'");
        sql.append("   AND u.created_date >= :startDate AND u.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.name LIKE :search OR u.email LIKE :search OR u.nickname LIKE :search)");
        }

        sql.append(" GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,");
        sql.append(" u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,");
        sql.append(" u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date");
        sql.append(" HAVING COUNT(ep.events_id) = 1");
        sql.append(" ORDER BY ").append(getSortClause(sort, "u", SortContext.USER));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findVipUserDetailsWithSortAndSearch(String sort, String search,
                                                               LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(USER_SELECT_COLUMNS);
        sql.append(", COUNT(ep.events_id) AS event_count");
        sql.append(" FROM users u");
        sql.append(" JOIN event_participations ep ON u.users_id = ep.users_id");
        sql.append("   AND ep.participation_role IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')");
        sql.append(" WHERE u.status = 'ACTIVE'");
        sql.append("   AND u.created_date >= :startDate AND u.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.name LIKE :search OR u.email LIKE :search OR u.nickname LIKE :search)");
        }

        sql.append(" GROUP BY u.users_id, u.email, u.name, u.nickname, u.provider, u.provider_id,");
        sql.append(" u.service_policy_agreement, u.privacy_policy_agreement, u.marketing_policy_agreement,");
        sql.append(" u.sleep_start_time, u.sleep_end_time, u.language, u.created_date, u.updated_date");
        sql.append(" HAVING COUNT(ep.events_id) >= 5");
        sql.append(" ORDER BY ").append(getSortClause(sort, "u", SortContext.VIP));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> findZeroParticipantEventDetailsWithSortAndSearch(String sort, String search,
                                                                            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.events_id, e.title, e.category, e.start_time, e.end_time, e.created_date,");
        sql.append(" u.users_id, u.email, u.name, u.nickname,");
        sql.append(" DATEDIFF(NOW(), e.created_date) AS days_since_created");
        sql.append(" FROM users u");
        sql.append(" JOIN event_participations ep ON u.users_id = ep.users_id");
        sql.append(" JOIN events e ON ep.events_id = e.events_id");
        sql.append(" LEFT JOIN members m ON e.events_id = m.events_id");
        sql.append(" WHERE ep.participation_role IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')");
        sql.append("   AND e.status = 'ACTIVE'");
        sql.append("   AND e.created_date < DATE_SUB(NOW(), INTERVAL 3 DAY)");
        sql.append("   AND e.created_date >= :startDate AND e.created_date < :endDate");

        if (search != null && !search.isBlank()) {
            sql.append(" AND (e.title LIKE :search OR u.name LIKE :search OR u.email LIKE :search)");
        }

        sql.append(" GROUP BY e.events_id, e.title, e.category, e.start_time, e.end_time, e.created_date,");
        sql.append(" u.users_id, u.email, u.name, u.nickname");
        sql.append(" HAVING COUNT(m.members_id) = 0");
        sql.append(" ORDER BY ").append(getSortClause(sort, "e", SortContext.EVENT));

        Query query = entityManager.createNativeQuery(sql.toString());
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    /**
     * 정렬 컨텍스트 enum
     */
    private enum SortContext { USER, VIP, EVENT }

    /**
     * 통합 정렬 조건 생성
     * @param sort 정렬 옵션 (created_date_desc, created_date_asc, name_asc, name_desc)
     * @param alias 테이블 별칭 (u, e 등)
     * @param context 정렬 컨텍스트
     */
    private String getSortClause(String sort, String alias, SortContext context) {
        if (sort == null) sort = "created_date_desc";

        String nameField = context == SortContext.EVENT ? "title" : "name";

        return switch (sort) {
            case "created_date_asc" -> alias + ".created_date ASC";
            case "name_asc" -> alias + "." + nameField + " ASC";
            case "name_desc" -> alias + "." + nameField + " DESC";
            default -> {
                if (context == SortContext.VIP) {
                    yield "event_count DESC, " + alias + ".created_date DESC";
                }
                yield alias + ".created_date DESC";
            }
        };
    }
}
